package com.example.reproductordemsica;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    EditText etSearch;
    ImageButton btnSearch, btnPlay, btnNext, btnPrev;
    SeekBar seekBar;
    Button btnAdd, btnEdit, btnDelete;
    DBHelper dbHelper;
    List<Song> songs, filtered;
    SongAdapter adapter;
    MediaPlayer player;
    int currentIndex = -1;
    Handler uiHandler = new Handler();
    ActivityResultLauncher<String[]> mGetContent;
    DatabaseReference firebaseSongs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().subscribeToTopic("news")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Suscrito a noticias", Toast.LENGTH_SHORT).show();
                    }
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        String token = t.getResult();
                    }
                });

        listView = findViewById(R.id.listView);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnPlay   = findViewById(R.id.btnPlay);
        btnNext   = findViewById(R.id.btnNext);
        btnPrev   = findViewById(R.id.btnPrev);
        seekBar   = findViewById(R.id.seekBar);
        btnAdd    = findViewById(R.id.btnAdd);
        btnEdit   = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        dbHelper  = new DBHelper(this);

        player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        player.setOnCompletionListener(mp -> btnPlay.setImageResource(R.drawable.ic_play));

        firebaseSongs = FirebaseDatabase.getInstance().getReference("songs");

        mGetContent = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        importSong(uri);
                    }
                }
        );

        loadSongs();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filtered.clear();
                for (Song sng : songs)
                    if (sng.title.toLowerCase()
                            .contains(s.toString().toLowerCase()))
                        filtered.add(sng);
                adapter.notifyDataSetChanged();
            }
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {}
        });

        listView.setOnItemClickListener((p, v, pos, id) -> {
            currentIndex = pos;
            playSong(filtered.get(pos).path);
        });

        btnPlay.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlay.setImageResource(R.drawable.ic_play);
            } else if (currentIndex >= 0) {
                player.start();
                btnPlay.setImageResource(R.drawable.ic_pause);
            }
        });

        btnNext.setOnClickListener(v -> skip(1));
        btnPrev.setOnClickListener(v -> skip(-1));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (currentIndex >= 0) player.seekTo(s.getProgress());
            }
        });

        btnAdd.setOnClickListener(v -> mGetContent.launch(new String[]{"audio/*"}));
        btnEdit.setOnClickListener(v -> crud("Editar", this::editSong));
        btnDelete.setOnClickListener(v -> crud("Eliminar", this::deleteSong));

        uiHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (player.isPlaying())
                    seekBar.setProgress(player.getCurrentPosition());
                uiHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
    }

    void loadSongs() {
        songs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,title,path FROM songs", null);
        while (c.moveToNext())
            songs.add(new Song(c.getInt(0), c.getString(1), c.getString(2)));
        c.close();
        filtered = new ArrayList<>(songs);
        adapter = new SongAdapter(filtered);
        listView.setAdapter(adapter);
    }

    void playSong(String path) {
        try {
            player.reset();
            player.setDataSource(this, Uri.parse(path));
            player.prepare();
            seekBar.setMax(player.getDuration());
            player.start();
            btnPlay.setImageResource(R.drawable.ic_pause);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void skip(int d) {
        int ni = currentIndex + d;
        if (ni >= 0 && ni < filtered.size()) {
            currentIndex = ni;
            playSong(filtered.get(ni).path);
        }
    }

    @SuppressLint("Range")
    void importSong(Uri u) {
        String title = "";
        Cursor c0 = getContentResolver().query(u, null, null, null, null);
        if (c0 != null && c0.moveToFirst()) {
            title = c0.getString(c0.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            c0.close();
        }
        String path = u.toString();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c1 = db.rawQuery("SELECT 1 FROM songs WHERE path=?", new String[]{path});
        if (c1.moveToFirst()) {
            c1.close();
            return;
        }
        c1.close();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("path", path);
        long localId = db.insert("songs", null, v);
        if (localId == -1) return;
        Song song = new Song((int)localId, title, path);
        firebaseSongs.child(String.valueOf(localId)).setValue(song);
        loadSongs();
    }

    void deleteSong() {
        if (currentIndex < 0) return;
        int idToDelete = filtered.get(currentIndex).id;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("songs", "id=?", new String[]{"" + idToDelete});
        firebaseSongs.child(String.valueOf(idToDelete)).removeValue();
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM songs", null);
        if (countCursor.moveToFirst()) {
            int count = countCursor.getInt(0);
            if (count == 0) {
                db.execSQL("DELETE FROM sqlite_sequence WHERE name='songs'");
            }
        }
        countCursor.close();
        currentIndex = -1;
        loadSongs();
    }

    void editSong() {
        if (currentIndex < 0) return;
        EditText et = new EditText(this);
        et.setText(filtered.get(currentIndex).title);
        new AlertDialog.Builder(this)
                .setTitle("Nuevo nombre")
                .setView(et)
                .setPositiveButton("OK", (d,i) -> {
                    String newTitle = et.getText().toString();
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("title", newTitle);
                    int id = filtered.get(currentIndex).id;
                    db.update("songs", cv, "id=?", new String[]{""+id});
                    firebaseSongs.child(String.valueOf(id)).child("title").setValue(newTitle);
                    loadSongs();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void crud(String t, Runnable fn) {
        new AlertDialog.Builder(this)
                .setTitle(t + " canción")
                .setMessage("¿Seguro?")
                .setPositiveButton("Sí", (d,i) -> fn.run())
                .setNegativeButton("No", null)
                .show();
    }

    public static class Song {
        public int id;
        public String title;
        public String path;
        public Song() {}
        public Song(int i, String t, String p) {
            id = i; title = t; path = p;
        }
    }

    class SongAdapter extends BaseAdapter {
        List<Song> data;
        SongAdapter(List<Song> d) { data = d; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return data.get(i).id; }

        @Override
        public View getView(int i, View cv, ViewGroup p) {
            if (cv == null) {
                cv = getLayoutInflater().inflate(R.layout.item_song, p, false);
            }
            ImageView img = cv.findViewById(R.id.imgCover);
            TextView tv  = cv.findViewById(R.id.tvTitle);
            Song s = data.get(i);
            tv.setText(s.title);
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(s.path, new java.util.HashMap<>());
                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) {
                    img.setImageBitmap(
                            BitmapFactory.decodeStream(new ByteArrayInputStream(art))
                    );
                } else {
                    img.setImageResource(R.drawable.ic_music_note);
                }
            } catch (Exception e) {
                img.setImageResource(R.drawable.ic_music_note);
            }
            return cv;
        }
    }
}