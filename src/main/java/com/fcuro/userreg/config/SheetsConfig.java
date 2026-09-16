package com.fcuro.userreg.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Google Sheets APIクライアントの設定。
 *
 * 認証にはApplication Default Credentials (ADC) を使用する。
 * - ローカル開発: 環境変数 GOOGLE_APPLICATION_CREDENTIALS にサービスアカウントの鍵ファイルを指定
 * - Cloud Run: デプロイ時に指定したサービスアカウントの権限がメタデータサーバ経由で自動的に使われる
 */
@Configuration
public class SheetsConfig {

    @Value("${app.name:user-registration-system}")
    private String applicationName;

    @Bean
    public Sheets sheetsService() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(SheetsScopes.SPREADSHEETS);

        return new Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
