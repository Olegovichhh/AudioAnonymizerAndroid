package com.olegovichhh.audioanonymizer

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private var source: Uri? = null
    private var player: MediaPlayer? = null
    private lateinit var fileLabel: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(42,60,42,42); gravity=Gravity.CENTER_HORIZONTAL }
        fun label(s:String,size:Float)=TextView(this).apply{text=s;textSize=size;setPadding(0,14,0,14)}
        root.addView(label("Audio Anonymizer",28f))
        root.addView(label("Локальная обработка аудио • v0.1",15f))
        fileLabel=label("Аудиофайл не выбран",16f); root.addView(fileLabel)
        root.addView(Button(this).apply{text="Выбрать аудио";setOnClickListener{
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},10)
        }})
        root.addView(Button(this).apply{text="▶ Прослушать оригинал";setOnClickListener{play()}})
        root.addView(label("Pitch shift",18f)); val pitch=SeekBar(this).apply{max=24;progress=12}; root.addView(pitch)
        root.addView(label("Tempo / BPM",18f)); val tempo=SeekBar(this).apply{max=100;progress=50}; root.addView(tempo)
        root.addView(label("Granular",18f)); val grain=SeekBar(this).apply{max=100;progress=20}; root.addView(grain)
        status=label("Готово к работе",14f)
        root.addView(Button(this).apply{text="Обработать";setOnClickListener{
            status.text=if(source==null)"Сначала выберите аудио" else "DSP-движок — следующий этап. Pitch: "+(pitch.progress-12)+", Tempo: "+tempo.progress+", Granular: "+grain.progress
        }})
        root.addView(status)
        setContentView(ScrollView(this).apply{addView(root)})
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==10&&resultCode==RESULT_OK){source=data?.data;fileLabel.text=source?.let(::nameOf)?:"Аудио выбрано";status.text="Файл загружен"}}
    private fun nameOf(uri:Uri):String{contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i)};return "Аудиофайл"}
    private fun play(){val u=source?:run{status.text="Сначала выберите аудио";return};player?.release();player=MediaPlayer().apply{setDataSource(this@MainActivity,u);prepare();start()}}
    override fun onDestroy(){player?.release();super.onDestroy()}
}
