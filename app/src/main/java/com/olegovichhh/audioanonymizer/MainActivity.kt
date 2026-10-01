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
class MainActivity:Activity(){
 private var source:Uri?=null;private var processed:File?=null;private var pending:File?=null;private var player:MediaPlayer?=null
 private lateinit var status:TextView;private lateinit var file:TextView;private lateinit var pitch:SeekBar;private lateinit var tempo:SeekBar;private lateinit var grain:SeekBar;private lateinit var progress:ProgressBar
 override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,44,36,44);gravity=Gravity.CENTER_HORIZONTAL}
  fun txt(s:String,z:Float)=TextView(this).apply{text=s;textSize=z;setPadding(0,8,0,8)}
  fun btn(s:String,f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()}}
  root.addView(txt("Audio Anonymizer",26f));root.addView(txt("Local audio processor • v0.3",13f));file=txt("Файл не выбран",15f);root.addView(file)
  root.addView(btn("ВЫБРАТЬ АУДИО"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},10)})
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};row.addView(btn("▶ ОРИГИНАЛ"){playUri(source)},LinearLayout.LayoutParams(0,-2,1f));row.addView(btn("▶ РЕЗУЛЬТАТ"){processed?.let(::playFile)?:run{status.text="Сначала обработайте файл"}},LinearLayout.LayoutParams(0,-2,1f));root.addView(row)
  val pv=txt("Pitch: 0 st",15f);root.addView(pv);pitch=SeekBar(this).apply{max=24;progress=12;setOnSeekBarChangeListener(listener{p->pv.text="Pitch: "+(p-12)+" st"})};root.addView(pitch)
  val tv=txt("Tempo: 1.00×",15f);root.addView(tv);tempo=SeekBar(this).apply{max=100;progress=50;setOnSeekBarChangeListener(listener{p->tv.text=String.format("Tempo: %.2f×",.5+p/100.0)})};root.addView(tempo)
  val gv=txt("Granular: 0%",15f);root.addView(gv);grain=SeekBar(this).apply{max=100;setOnSeekBarChangeListener(listener{p->gv.text="Granular: "+p+"%"})};root.addView(grain)
  val presets=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("ЛЁГКИЙ" to intArrayOf(2,47,12),"СРЕДНИЙ" to intArrayOf(-3,44,28),"СИЛЬНЫЙ" to intArrayOf(-6,40,45)).forEach{pair->presets.addView(btn(pair.first){pitch.progress=pair.second[0]+12;tempo.progress=pair.second[1];grain.progress=pair.second[2]},LinearLayout.LayoutParams(0,-2,1f))};root.addView(presets)
  root.addView(btn("ОБРАБОТАТЬ"){process()});root.addView(btn("СОХРАНИТЬ WAV"){save()});progress=ProgressBar(this).apply{isIndeterminate=true;visibility=ProgressBar.GONE};root.addView(progress);status=txt("WAV / MP3 / M4A / FLAC через системный декодер",13f);root.addView(status);setContentView(ScrollView(this).apply{addView(root)})}
 private fun listener(f:(Int)->Unit)=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean)=f(p);override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
 private fun process(){val u=source?:run{status.text="Выберите аудио";return};progress.visibility=ProgressBar.VISIBLE;status.text="Обработка…";thread{try{val w=AudioDecoder.decode(this,u);val o=WavDsp.process(w,(pitch.progress-12).toFloat(),.5f+tempo.progress/100f,if(grain.progress==0)0 else 70,grain.progress/100f);val f=File(cacheDir,"processed.wav");f.outputStream().use{stream->WavDsp.write(o,stream)};processed=f;runOnUiThread{progress.visibility=ProgressBar.GONE;status.text="Готово • "+f.length()/1024+" KB"}}catch(e:Exception){runOnUiThread{progress.visibility=ProgressBar.GONE;status.text="Ошибка: "+e.message}}}}
 private fun save(){val f=processed?:run{status.text="Нет результата";return};pending=f;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/wav";putExtra(Intent.EXTRA_TITLE,"anonymized.wav")},20)}
 override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(c!=RESULT_OK)return;if(r==10){source=d?.data;file.text=source?.let(::name)?:"Аудио";status.text="Файл готов"}else if(r==20){val u=d?.data;val f=pending;if(u!=null&&f!=null){contentResolver.openOutputStream(u)?.use{o->f.inputStream().use{i->i.copyTo(o)}};status.text="WAV сохранён"}}}
 private fun name(u:Uri):String{contentResolver.query(u,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i)};return "audio"}
 private fun playUri(u:Uri?){u?:run{status.text="Выберите аудио";return};player?.release();player=MediaPlayer().apply{setDataSource(this@MainActivity,u);prepare();start()}}
 private fun playFile(f:File){player?.release();player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start()}}
 override fun onDestroy(){player?.release();super.onDestroy()}
}