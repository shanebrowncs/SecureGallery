package ca.shanebrown.securegallery.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    /**
     * Stores given password hash and salt to database
     * @param hash Password hash to store
     * @param salt Hash salt to store
     * @return True on success, false on failure
     */
    public boolean savePassword(byte[] hash, byte[] salt){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.putNull("id");
        values.put("secret", hash);
        values.put("salt", salt);

        if(getPassword() == null){ // Insert password row
            return db.insert("secrets", null, values) > 0;
        }else{                     // Update password row
            return db.update("secrets", values, "1", new String[]{}) > 0;
        }
    }

    /**
     * Retrieves password hash and salt from database
     * @return Key/Value store containing hash and salt
     */
    public ContentValues getPassword(){
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();

        Cursor cur = db.rawQuery("SELECT `secret`,`salt` FROM `secrets` WHERE 1", new String[]{});

        // No rows in database, password isn't stored
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
