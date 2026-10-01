package com.olegovichhh.audioanonymizer

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private var source: Uri?=null
    private var processed: File?=null
    private var player:MediaPlayer?=null
    private lateinit var fileLabel:TextView
    private lateinit var status:TextView
    private lateinit var pitchValue:TextView
    private lateinit var tempoValue:TextView
    private lateinit var granularValue:TextView

    override fun onCreate(b:Bundle?){super.onCreate(b)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(42,60,42,42);gravity=Gravity.CENTER_HORIZONTAL}
        fun label(s:String,z:Float)=TextView(this).apply{text=s;textSize=z;setPadding(0,10,0,10)}
        fun button(s:String,fn:()->Unit)=Button(this).apply{text=s;setOnClickListener{fn()}}
        root.addView(label("Audio Anonymizer",28f));root.addView(label("Локальная обработка WAV • v0.2",14f))
        fileLabel=label("WAV-файл не выбран",16f);root.addView(fileLabel)
        root.addView(button("Выбрать WAV"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/wav"},10)})
        root.addView(button("▶ Оригинал"){playUri(source)})
        root.addView(label("Pitch shift",18f));pitchValue=label("0 semitones",14f);root.addView(pitchValue)
        val pitch=SeekBar(this).apply{max=24;progress=12;setOnSeekBarChangeListener(listener{pitchValue.text="${it-12} semitones"})};root.addView(pitch)
        root.addView(label("Tempo",18f));tempoValue=label("1.00×",14f);root.addView(tempoValue)
        val tempo=SeekBar(this).apply{max=100;progress=50;setOnSeekBarChangeListener(listener{tempoValue.text=String.format("%.2f×",0.5+it/100.0)})};root.addView(tempo)
        root.addView(label("Granular strength",18f));granularValue=label("0%",14f);root.addView(granularValue)
        val grain=SeekBar(this).apply{max=100;progress=0;setOnSeekBarChangeListener(listener{granularValue.text="$it%"})};root.addView(grain)
        root.addView(button("ОБРАБОТАТЬ"){process(pitch.progress-12,(0.5f+tempo.progress/100f),grain.progress)})
        root.addView(button("▶ Результат"){processed?.let{playFile(it)}?:run{status.text="Сначала обработайте файл"}})
        root.addView(button("Сохранить WAV"){save()})
        status=label("Обработка выполняется на устройстве",14f);root.addView(status)
        setContentView(ScrollView(this).apply{addView(root)})
    }
    private fun listener(fn:(Int)->Unit)=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean)=fn(p);override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
    private fun process(p:Int,t:Float,g:Int){val u=source?:run{status.text="Выберите WAV";return};status.text="Обработка…"
        thread{try{val wav=contentResolver.openInputStream(u)!!.use{WavDsp.read(it)};val out=WavDsp.process(wav,p.toFloat(),t,if(g==0)0 else 80,g/100f);val f=File(cacheDir,"processed.wav");f.outputStream().use{WavDsp.write(out,it)};processed=f;runOnUiThread{status.text="Готово: ${f.length()/1024} KB"}}catch(e:Exception){runOnUiThread{status.text="Ошибка: ${e.message}"}}}
    }
    private fun save(){val f=processed?:run{status.text="Нет обработанного файла";return};startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/wav";putExtra(Intent.EXTRA_TITLE,"anonymized.wav")},20);pendingSave=f}
    private var pendingSave:File?=null
    private fun nameOf(u:Uri):String{contentResolver.query(u,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i)};return "audio.wav"}
    private fun playUri(u:Uri?){u?:run{status.text="Выберите WAV";return};player?.release();player=MediaPlayer().apply{setDataSource(this@MainActivity,u);prepare();start()}}
    private fun playFile(f:File){player?.release();player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start()}}
    override fun onDestroy(){player?.release();super.onDestroy()}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==10&&resultCode==RESULT_OK){source=data?.data;fileLabel.text=source?.let(::nameOf)?:"WAV";status.text="Файл готов"}else if(requestCode==20&&resultCode==RESULT_OK){val u=data?.data;val f=pendingSave;if(u!=null&&f!=null){contentResolver.openOutputStream(u)?.use{o->f.inputStream().use{it.copyTo(o)}};status.text="WAV сохранён"}}}
}
