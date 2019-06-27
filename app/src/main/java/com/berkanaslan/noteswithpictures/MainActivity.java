package com.berkanaslan.noteswithpictures;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import java.lang.reflect.Array;
import java.sql.Blob;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // Global variables
    ListView listView;
    static ArrayList<Bitmap> imageArray; //Veritabanından çekilecek image'in array'i.
    static ArrayList<String> titleArray; //Veritabanından çekilecek note title'ın array'i.
    static ArrayList<String> noteArray; //Veritabanından çekilecek note text'in array'i.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //Activity'de üç noktalı option menü oluşturur.

        MenuInflater menuInflater = getMenuInflater();
        //Inflater sayesinde "inflate" kullanabiliyoruz. Öncesinde "menu" adında directory oluşturup File > New > Menu Resource yapmak gerekiyor.
        menuInflater.inflate(R.menu.add_note, menu); //menu directory'de add_note adını verdik, oradan alıyoruz.

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { //Hazır metod, menu item'i selected olduğunda demek.

        if (item.getItemId() == R.id.add_note) { //Eğer selected item id'si bizim add_note itemi id'sine eşitse

            Intent intent = new Intent(getApplicationContext(), Main2Activity.class); //Main2Activity intent'i oluştur.
            intent.putExtra("info", "new"); //Intent'e "new" verisi yolla.
            startActivity(intent); //Intent start et.


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbax ID Tanımlamaları
        listView = (ListView)findViewById(R.id.noteList);

        //ArrayList tanımlamaları
        titleArray = new ArrayList<String>();
        noteArray = new ArrayList<String>();
        imageArray = new ArrayList<Bitmap>();

        //ArrayAdapter oluşturup titleArray ile listView'a bağlıyoruz.
        final ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titleArray);
        listView.setAdapter(arrayAdapter);

        //Database kodlarını app çökmesin diye try & catch'de deniyoruz.
        try {

            Main2Activity.noteDatabase = this.openOrCreateDatabase("notes", MODE_PRIVATE, null); //Database'yi açtık veya oluşturduk.
            Main2Activity.noteDatabase.execSQL("CREATE TABLE IF NOT EXISTS noteswithimage (title VARCHAR, note VARCHAR, image BLOB)"); //Tablomuz yoksa oluşturduk.

            Cursor cursor = Main2Activity.noteDatabase.rawQuery("SELECT * FROM noteswithimage", null); //Tablonun bütün verilerini çektik.

            //Hücre indexlerini değişkenlere kaydet.
            int titleIX = cursor.getColumnIndex("title");
            int noteIx = cursor.getColumnIndex("note");
            int imageIx = cursor.getColumnIndex("image");

            cursor.moveToFirst(); //Satırı okuduktan sonra başa dön.

            while (cursor != null) { //Cursor boş değilse (işaretleyici, her hücreyi dolaşmak gibi)

                //Database indexlerini dizilere ekle
                titleArray.add(cursor.getString(titleIX));
                noteArray.add(cursor.getString(noteIx));
                byte[] byteArray = cursor.getBlob(imageIx); //Image için byte dizisi oluşturmamız gerekiyor. getBlob şart.

                Bitmap image = BitmapFactory.decodeByteArray(byteArray,0, byteArray.length);
                imageArray.add(image);

                cursor.moveToNext(); //Yeni hücreye geç.

                arrayAdapter.notifyDataSetChanged(); //Veride değişiklik olursa notify at.

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { //ListView'da item'e tıklandığında...
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(),Main2Activity.class); //2nd activity'e yolla.
                intent.putExtra("info", "old"); //2nd activity'de save butonu gözüksün mü? old unvisible, new visible

                //İkinci activity'de seçilen item'ın indexlerini metot'tan alıp array dizisi ile intent.put ediyoruz.
                intent.putExtra("title",titleArray.get(i));
                intent.putExtra("note",noteArray.get(i));
                intent.putExtra("position", i); //Burada ile dizi indexi ile put yerine, index yolluyoruz sadece.


                startActivity(intent); //Activity'ye yolladık.

            }
        });

        /* Kaydedilmiş listView itemini uzun basarak silmek için uzun basma metodu ekledim.
        *  Bu hazır metot değil. Manuel Import etmek gerekebilir. */
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3) {

                final String deleteTitle = titleArray.get(index); /*listView'da titleArray elemanları olduğu için üst satırdaki index loop'undan
                indexisini buldum değişkene attım */

                //Alert dialog ile yes or no sorusu sorduk.
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

                alert.setTitle("Are you sure?");
                alert.setMessage("Are you sure want to delete it?");

               alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        try {
                            //Database bağlantısı
                            Main2Activity.noteDatabase = MainActivity.this.openOrCreateDatabase("notes", MODE_PRIVATE, null);

                            String sqlString = "DELETE FROM noteswithimage WHERE title = (?)";
                            SQLiteStatement statement = Main2Activity.noteDatabase.compileStatement(sqlString);
                            statement.bindString(1,deleteTitle); //SQL kodu ile değişkeni bağladık. (sqlString değişkeni)
                            statement.execute(); //Çalıştırdık.

                            Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_LONG).show(); //Silindiğini bildiren Toast message.

                            //Silindikten sonra listView itemlerini refreshledik.
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });

               //No butonuna basılıacaksa Toast Message "Canceled" verecek.
                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        Toast.makeText(MainActivity.this, "Canceled.", Toast.LENGTH_SHORT).show();

                    }
                });

                alert.show(); //Alert görüntüle.

                return true; //OnLongClickListener metoduna dahil. Hazır gelmiyor, manuel eklemek gerekebilir.
            }
        });

    }
}
