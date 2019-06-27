package com.berkanaslan.noteswithpictures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Main2Activity extends AppCompatActivity {

    //Global variables
    ImageView imageView;
    EditText titleText;
    EditText noteText;
    Button saveButton;
    Bitmap selectedImage;
    static SQLiteDatabase noteDatabase; //Başka activity'de Main2Activity.noteDatabase olarak kullanılır.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        //ID Tanımlama
        imageView = (ImageView)findViewById(R.id.imageView);
        titleText = (EditText)findViewById(R.id.editText);
        noteText = (EditText)findViewById(R.id.editText2);
        saveButton = (Button)findViewById(R.id.button);



        // Yeni resim eklenecekse save butonu gözükecek, eski nota bakılıyorsa gözükmeyecek.
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equalsIgnoreCase("new")) { //Buton gözüksün istiyoruz. Yeni kayıt yapılacak demek

            saveButton.setVisibility(View.VISIBLE);
            titleText.setText(""); //Title sıfırla.
            noteText.setText(""); //Note text sıfırla
            Bitmap background = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.background); //Select image sıfırla. "tap to select yazan"
        } else {

            //MainActivity onItemClick'de put edilen verileri tool'lara eşitle. Image için index için "i" yolladık.
            titleText.setText(intent.getStringExtra("title"));
            noteText.setText(intent.getStringExtra("note"));
            int i = intent.getIntExtra("position", 0);
            imageView.setImageBitmap(MainActivity.imageArray.get(i));

            saveButton.setVisibility(View.INVISIBLE);
        }


    }

    public void selectimage (View view) { //ImageView onClick metodu

        /* İlk olarak Manifes dosyasından READ_EXTERNAL_STORAGE iznini tanımlıyoruz. Ve alt satırda dosya izninin verilip
         * verilmediğini kontrol ediyoruz */
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            //İstek kodunu 2 atadık. String dizisinin içerisine permission'u yazdık.

        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,1);

            //Eğer izin verildiyse action_pick intent'i çalıştırılacak ve mediaStore'a ulaşacağız.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 2) { //İzin verildiyse

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(Intent.ACTION_PICK, Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,1); /*Image kaynagı eklerken kaynak belirtmemiz gerekiyor.*/

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) { //Seçilen data boş değilse ve metot request = 1 ise

            Uri image = data.getData(); //Uri URL gibi düşünülebilir. metot değişken "data", image'a atılır.

            try { //App çökmesin diye try & catch
                selectedImage = Images.Media.getBitmap(this.getContentResolver(), image); //Global bitmap değişkenine Uri image'yi atıyoruz.
                imageView.setImageBitmap(selectedImage); //Yazılan bitmap değişkenini imageView'a koyuyoruz; default'u background olan resim değişecek.
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    public void save(View view) { //SAVE butonuna tıklanırsa

        //Title ile Text değişkenlere atandı.
        String noteName = titleText.getText().toString();
        String note = noteText.getText().toString();

        /*Image kaydederken ByteArrayOutputStream kullanıyoruz.
         *compress ile hangi formatta, hangi kalitede sıkıştırılacağını belirtiyoruz.
         *byte dizisi oluşturup outputstream ile kayta kaydediyoruz. */
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        selectedImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            noteDatabase = this.openOrCreateDatabase("notes", MODE_PRIVATE,null);
            noteDatabase.execSQL("CREATE TABLE IF NOT EXISTS noteswithimage (title VARCHAR, note VARCHAR, image BLOB)");

            //Statement'de kullanılan değişkenler save metodundan geliyor.
            String sqlString = "INSERT INTO noteswithimage (title, note, image) VALUES (?, ?, ?)";
            SQLiteStatement statement = noteDatabase.compileStatement(sqlString);
            statement.bindString(1,noteName);
            statement.bindString(2,note);
            statement.bindBlob(3,byteArray);
            statement.execute();


        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent); //Kaydedilme işleminden sonra ilk activity'ye dönüyoruz.

    }
}
