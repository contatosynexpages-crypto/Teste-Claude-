package com.minhasmusicas.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

/**
 * Baixa o áudio de um link do YouTube com o yt-dlp (biblioteca youtubedl-android).
 * Também recebe links compartilhados de outros apps ("Compartilhar" → Minhas Músicas).
 */
@CapacitorPlugin(name = "YouTube")
public class YouTubePlugin extends Plugin {
    private static final String TAG = "YouTubePlugin";
    private static final String PROCESS_ID = "yt-download";
    private static final long UPDATE_INTERVAL_MS = 24L * 60 * 60 * 1000;

    // Uma tarefa por vez: iniciar, atualizar o yt-dlp e baixar nunca rodam juntos.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean ready = false;

    @Override
    public void load() {
        executor.execute(() -> {
            if (ensureInit()) updateIfDue();
        });
        handleShare(getActivity().getIntent());
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        handleShare(intent);
    }

    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) return;
        intent.setAction(null); // não processar o mesmo compartilhamento de novo
        JSObject data = new JSObject();
        data.put("text", text);
        notifyListeners("shared", data, true);
    }

    private synchronized boolean ensureInit() {
        if (ready) return true;
        try {
            YoutubeDL.getInstance().init(getContext());
            FFmpeg.getInstance().init(getContext());
            ready = true;
        } catch (Exception e) {
            Log.e(TAG, "falha ao iniciar o yt-dlp", e);
        }
        return ready;
    }

    // O YouTube muda com frequência; manter o yt-dlp atualizado evita downloads quebrados.
    private void updateIfDue() {
        SharedPreferences prefs = getContext().getSharedPreferences("youtube", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("lastUpdate", 0) < UPDATE_INTERVAL_MS) return;
        try {
            YoutubeDL.getInstance().updateYoutubeDL(getContext(), YoutubeDL.UpdateChannel._STABLE);
            prefs.edit().putLong("lastUpdate", now).apply();
        } catch (Exception e) {
            Log.w(TAG, "falha ao atualizar o yt-dlp", e);
        }
    }

    private File downloadDir() {
        File dir = new File(getContext().getFilesDir(), "youtube");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    @PluginMethod
    public void download(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.trim().isEmpty()) {
            call.reject("Cole um link do YouTube");
            return;
        }
        executor.execute(() -> {
            if (!ensureInit()) {
                call.reject("Não foi possível iniciar o baixador");
                return;
            }
            YoutubeDLRequest req = new YoutubeDLRequest(url.trim());
            req.addOption("--no-playlist");
            req.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best");
            req.addOption("-o", downloadDir().getAbsolutePath() + "/%(id)s.%(ext)s");
            req.addOption("--no-simulate");
            req.addOption("--progress");
            req.addOption("--newline");
            req.addOption("--print", "after_move:%(.{id,title,track,artist,uploader,channel,duration,filepath})j");
            try {
                YoutubeDLResponse res = YoutubeDL.getInstance().execute(req, PROCESS_ID, false, (progress, eta, line) -> {
                    JSObject p = new JSObject();
                    p.put("progress", progress.doubleValue());
                    p.put("eta", eta.longValue());
                    notifyListeners("progress", p);
                    return Unit.INSTANCE;
                });
                JSONObject info = lastJsonLine(res.getOut());
                if (info == null) {
                    call.reject("O YouTube não retornou o áudio");
                    return;
                }
                String path = str(info, "filepath");
                if (path.isEmpty() || !new File(path).exists()) {
                    call.reject("Arquivo baixado não encontrado");
                    return;
                }
                JSObject result = new JSObject();
                result.put("path", path);
                result.put("id", str(info, "id"));
                result.put("title", str(info, "title"));
                result.put("track", str(info, "track"));
                result.put("artist", str(info, "artist"));
                result.put("uploader", str(info, "uploader").isEmpty() ? str(info, "channel") : str(info, "uploader"));
                result.put("duration", info.optDouble("duration", 0));
                String cover = coverDataUrl(str(info, "id"));
                if (cover != null) result.put("cover", cover);
                call.resolve(result);
            } catch (YoutubeDL.CanceledException e) {
                call.reject("Download cancelado", "CANCELED");
            } catch (Exception e) {
                Log.e(TAG, "falha no download", e);
                call.reject(friendlyError(e.getMessage()));
            }
        });
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        YoutubeDL.getInstance().destroyProcessById(PROCESS_ID);
        call.resolve();
    }

    // O app copia o áudio para a biblioteca e depois apaga o arquivo baixado.
    @PluginMethod
    public void deleteFile(PluginCall call) {
        String path = call.getString("path");
        if (path != null) {
            File f = new File(path);
            if (f.getAbsolutePath().startsWith(downloadDir().getAbsolutePath())) f.delete();
        }
        call.resolve();
    }

    private static String str(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "");
    }

    private static JSONObject lastJsonLine(String out) {
        String[] lines = out.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String l = lines[i].trim();
            if (l.startsWith("{")) {
                try {
                    return new JSONObject(l);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static String friendlyError(String msg) {
        if (msg == null) return "Falha no download";
        for (String l : msg.split("\n")) {
            if (l.contains("ERROR:")) {
                String e = l.substring(l.indexOf("ERROR:") + 6).trim();
                if (e.contains("Unsupported URL") || e.contains("is not a valid URL")) return "Link inválido";
                if (e.contains("Private video")) return "Vídeo privado";
                if (e.contains("Video unavailable")) return "Vídeo indisponível";
                if (e.contains("Sign in")) return "O YouTube pediu login para este vídeo";
                return e.length() > 160 ? e.substring(0, 160) + "…" : e;
            }
        }
        return "Falha no download. Verifique a internet e o link.";
    }

    // Capa quadrada a partir da miniatura do vídeo (corta as bordas do 16:9).
    private static String coverDataUrl(String id) {
        if (id.isEmpty()) return null;
        for (String name : new String[]{"maxresdefault.jpg", "hqdefault.jpg"}) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://i.ytimg.com/vi/" + id + "/" + name).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                if (c.getResponseCode() != 200) continue;
                Bitmap bmp;
                try (InputStream in = c.getInputStream()) {
                    bmp = BitmapFactory.decodeStream(in);
                }
                if (bmp == null) continue;
                int w = bmp.getWidth(), h = bmp.getHeight();
                int side = Math.min(h, w * 9 / 16);
                Bitmap sq = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);
                if (side > 512) sq = Bitmap.createScaledBitmap(sq, 512, 512, true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                sq.compress(Bitmap.CompressFormat.JPEG, 85, out);
                return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) {
                Log.w(TAG, "falha ao baixar a capa", e);
            }
        }
        return null;
    }
}
