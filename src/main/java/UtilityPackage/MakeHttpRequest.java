package UtilityPackage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MakeHttpRequest {
    private static final String urlStringFireBasePush =
            "http://localhost:8080/NotifierService/Notify/Notification/User";
    private String charset = StandardCharsets.UTF_8.name();

    private final Map<String,String> paramMap;

    public MakeHttpRequest(final Map<String,String> paramMap){
        this.paramMap = paramMap;
    }

    public void makePostRequestToFireBase() throws IOException {
        HttpURLConnection toNotificationServer = (HttpURLConnection) new URL(MakeHttpRequest.urlStringFireBasePush)
                .openConnection();
        try(AutoCloseable ignored = toNotificationServer::disconnect){
            toNotificationServer.setDoInput(true);
            toNotificationServer.addRequestProperty("Accept-Charset",charset);
            toNotificationServer.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            try(OutputStream streamWriter = toNotificationServer.getOutputStream()){
                streamWriter.write(makeQueryString().getBytes(charset));
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO : debug
        }
    }

    private String makeQueryString() throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        for (HashMap.Entry<String,String> entry : this.paramMap.entrySet()){
            String param = URLEncoder.encode(entry.getKey(),charset);
            String value = URLEncoder.encode(entry.getValue(),charset);
            stringBuilder.append(param).append("&").append(value);
        }
        return stringBuilder.toString();
    }

}
