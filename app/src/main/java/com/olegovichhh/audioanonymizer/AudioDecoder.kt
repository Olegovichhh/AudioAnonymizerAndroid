package com.olegovichhh.audioanonymizer
import android.content.Context
import android.media.*
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
object AudioDecoder{
 fun decode(ctx:Context,uri:Uri):WavDsp.Wav{
  val ex=MediaExtractor();var codec:MediaCodec?=null
  try{
   ex.setDataSource(ctx,uri,null);var track=-1;var fmt:MediaFormat?=null
   for(i in 0 until ex.trackCount){val f=ex.getTrackFormat(i);if(f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true){track=i;fmt=f;break}}
   require(track>=0&&fmt!=null){"Аудиодорожка не найдена"};ex.selectTrack(track);val format=fmt!!
   val mime=format.getString(MediaFormat.KEY_MIME)!!;codec=MediaCodec.createDecoderByType(mime);codec.configure(format,null,null,0);codec.start()
   val info=MediaCodec.BufferInfo();val pcm=ByteArrayOutputStream();var inputDone=false;var outputDone=false
   var rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE);var ch=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);var encoding=AudioFormat.ENCODING_PCM_16BIT
   while(!outputDone){
    if(!inputDone){val n=codec.dequeueInputBuffer(10000);if(n>=0){val b=codec.getInputBuffer(n)!!;b.clear();val size=ex.readSampleData(b,0);if(size<0){codec.queueInputBuffer(n,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true}else{codec.queueInputBuffer(n,0,size,ex.sampleTime,0);ex.advance()}}}
    val n=codec.dequeueOutputBuffer(info,10000)
    if(n>=0){if(info.size>0){val b=codec.getOutputBuffer(n)!!;b.position(info.offset);b.limit(info.offset+info.size);val a=ByteArray(info.size);b.get(a);pcm.write(a)};codec.releaseOutputBuffer(n,false);if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0)outputDone=true}
    else if(n==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){val f=codec.outputFormat;rate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);ch=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);if(f.containsKey(MediaFormat.KEY_PCM_ENCODING))encoding=f.getInteger(MediaFormat.KEY_PCM_ENCODING)}
   }
   val bytes=pcm.toByteArray();require(bytes.isNotEmpty()){"Декодер не вернул PCM"}
   val samples=when(encoding){
    AudioFormat.ENCODING_PCM_FLOAT->{val bb=java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);ShortArray(bytes.size/4){(bb.float.coerceIn(-1f,1f)*32767f).toInt().toShort()}}
    else->{val bb=java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);ShortArray(bytes.size/2){bb.short}}
   }
   return WavDsp.Wav(rate,ch,samples)
  }finally{try{codec?.stop()}catch(_:Exception){};try{codec?.release()}catch(_:Exception){};ex.release()}
 }
}