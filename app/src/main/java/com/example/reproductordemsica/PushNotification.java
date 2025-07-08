package com.example.reproductordemsica;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class PushNotification extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "default_channel";
    private static final String TAG = "PushNotification";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // 1) Mostrar en logcat
        Log.d(TAG, "Nuevo FCM token: " + token);

        // 2) Guardar en Realtime Database bajo /tokens/{token} = true
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("tokens")
                .child(token);
        ref.setValue(true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token guardado en BD"))
                .addOnFailureListener(e -> Log.e(TAG, "Error guardando token", e));
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // 0) Log para depurar llegada de mensaje
        Log.d(TAG, "Llegó mensaje: " + remoteMessage.getData());

        // 1) Valores por defecto
        String title = "Nueva notificación";
        String body = "";

        // 2) Si viene sección notification…
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();

            // 3) …o si viene solo data
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        // 4) Crear canal (Android O+)
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones generales",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            nm.createNotificationChannel(channel);
        }

        // 5) Intent para abrir MainActivity al tocar
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // 6) Construir y mostrar notificación
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)  // o tu ic_notification
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), nb.build());
    }
}