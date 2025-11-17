package com.example.finalshield.Util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class FileUtils {
    public static MultipartBody.Part prepareFilePart(Context context, Uri fileUri) {
        String fileName = getFileName(context, fileUri);
        String mimeType = context.getContentResolver().getType(fileUri);

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        RequestBody fileBody = new InputStreamRequestBody(context, fileUri, MediaType.parse(mimeType));

        return MultipartBody.Part.createFormData("adjuntos", fileName, fileBody);
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private static class InputStreamRequestBody extends RequestBody {
        private final Context context;
        private final Uri uri;
        private final MediaType contentType;

        public InputStreamRequestBody(Context context, Uri uri, MediaType contentType) {
            this.context = context;
            this.uri = uri;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    throw new IOException("No se pudo abrir el InputStream: " + uri);
                }

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    sink.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
