package com.example.gym1.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class YouTubeService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);

    private final RestTemplate rest = new RestTemplate();

    // Toma primero la property youtube.api.key, si no existe, usa variable de entorno YOUTUBE_API_KEY
    @Value("${youtube.api.key:${YOUTUBE_API_KEY:}}")
    private String apiKey;

    // Extrae id de diferentes formatos de URL
    public static String extractYouTubeId(String url) {
        if (url == null) return null;
        String s = url.trim();
        try {
            if (s.contains("youtu.be/")) {
                String id = s.substring(s.lastIndexOf("youtu.be/") + 9).split("[?&]")[0];
                if (id.length() >= 6) return id;
            }
            if (s.contains("v=")) {
                String id = s.split("v=")[1].split("[&?#]")[0];
                if (id.length() >= 6) return id;
            }
            if (s.contains("/embed/")) {
                String id = s.substring(s.lastIndexOf("/embed/") + 7).split("[?&]")[0];
                if (id.length() >= 6) return id;
            }
            String[] parts = s.split("/");
            String last = parts[parts.length - 1].split("[?&]")[0];
            if (last.length() >= 6) return last;
        } catch (Exception e) {
            // no vomites excepción fuera; devuelve null
        }
        return null;
    }

    // Devuelve título (opcional)
    @SuppressWarnings("unchecked")
    public String getVideoTitle(String videoId) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("YouTube API key not configured (getVideoTitle)");
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "snippet")
                    .queryParam("id", videoId)
                    .queryParam("key", apiKey)
                    .toUriString();
            Map<String,Object> resp = rest.getForObject(url, Map.class);
            if (resp == null) return null;
            List<Object> items = (List<Object>) resp.get("items");
            if (items == null || items.isEmpty()) return null;
            Map<String,Object> first = (Map<String,Object>) items.get(0);
            Map<String,Object> snippet = (Map<String,Object>) first.get("snippet");
            return snippet != null ? (String) snippet.get("title") : null;
        } catch (RestClientException ex) {
            log.warn("YouTube API request failed (getVideoTitle): {}", ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> listChannelUploads(String channelId, int maxResultsTotal) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("YouTube API key not configured (listChannelUploads)");
            return Collections.emptyList();
        }
        try {
            // 1) obtener uploads playlist id
            String url1 = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                    .queryParam("part", "contentDetails")
                    .queryParam("id", channelId)
                    .queryParam("key", apiKey)
                    .toUriString();
            Map<String,Object> resp1 = rest.getForObject(url1, Map.class);
            if (resp1 == null) return Collections.emptyList();
            List<Object> items1 = (List<Object>) resp1.get("items");
            if (items1 == null || items1.isEmpty()) return Collections.emptyList();
            Map<String,Object> first = (Map<String,Object>) items1.get(0);
            Map<String,Object> contentDetails = (Map<String,Object>) first.get("contentDetails");
            if (contentDetails == null) return Collections.emptyList();
            Map<String,Object> related = (Map<String,Object>) contentDetails.get("relatedPlaylists");
            if (related == null) return Collections.emptyList();
            String uploads = (String) related.get("uploads");
            if (uploads == null) return Collections.emptyList();

            // 2) paginar playlistItems
            List<Map<String,Object>> out = new ArrayList<>();
            String nextPageToken = null;
            int fetched = 0;
            do {
                int pageMax = Math.min(50, maxResultsTotal - fetched);
                String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/playlistItems")
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("playlistId", uploads)
                        .queryParam("maxResults", pageMax)
                        .queryParam("key", apiKey)
                        .toUriString();
                if (nextPageToken != null) url += "&pageToken=" + nextPageToken;
                Map<String,Object> resp = rest.getForObject(url, Map.class);
                if (resp == null) break;
                List<Object> items = (List<Object>) resp.get("items");
                if (items != null) {
                    for (Object o : items) {
                        Map<String,Object> it = (Map<String,Object>) o;
                        Map<String,Object> snippet = (Map<String,Object>) it.get("snippet");
                        // Renombrada la variable para evitar conflicto con la variable contentDetails anterior
                        Map<String,Object> contentDetailsItem = (Map<String,Object>) it.get("contentDetails");
                        String vid = contentDetailsItem != null ? (String) contentDetailsItem.get("videoId") : null;
                        String title = snippet != null ? (String) snippet.get("title") : null;
                        Map<String,Object> m = new HashMap<>();
                        m.put("videoId", vid);
                        m.put("title", title);
                        m.put("thumbnailFallback", vid != null ? "https://i.ytimg.com/vi/" + vid + "/mqdefault.jpg" : null);
                        out.add(m);
                        fetched++;
                        if (fetched >= maxResultsTotal) break;
                    }
                }
                nextPageToken = (String) resp.get("nextPageToken");
            } while (nextPageToken != null && fetched < maxResultsTotal);

            return out;
        } catch (RestClientException ex) {
            log.warn("YouTube API request failed (listChannelUploads): {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}