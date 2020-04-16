package ca.shanebrown.securegallery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

public class SecureDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    public SecureDatabase(Context context){
        super(context, "SecureGallery", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String table_creation = "CREATE TABLE `secrets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `secret` BLOB NOT NULL, `salt` BLOB NOT NULL)";
        db.execSQL(table_creation);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != newVersion) {
            db.delete("secrets", "1", new String[]{});
            onCreate(db);
        }
    }

    public boolean savePassword(byte[] hash, byte[] salt){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.putNull("id");
        values.put("secret", hash);
        values.put("salt", salt);

        // INSERT
        if(getPassword() == null){
            return db.insert("secrets", null, values) > 0;

        }else{ // UPDATE
            return db.update("secrets", values, "1", new String[]{}) > 0;
        }
    }

    public ContentValues getPassword(){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();

        Cursor cur = db.rawQuery("SELECT `secret`,`salt` FROM `secrets` WHERE 1", new String[]{});

        // Oopsie woopsie, there's no password in here, shucks
        if(cur.getCount() < 1){
            return null;
        }

        cur.moveToFirst();

        byte[] hash = cur.getBlob(cur.getColumnIndex("secret"));
        byte[] salt = cur.getBlob(cur.getColumnIndex("salt"));

        values.put("secret", hash);
        values.put("salt", salt);

        cur.close();

        return values;
    }
}
