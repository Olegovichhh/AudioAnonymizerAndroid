package com.olegovichhh.audioanonymizer
import java.io.*
import kotlin.math.*
import kotlin.random.Random
object WavDsp{
 data class Wav(val sampleRate:Int,val channels:Int,val samples:ShortArray)
 fun read(input:InputStream):Wav{throw UnsupportedOperationException("Use AudioDecoder")}
 fun process(w:Wav,semitones:Float,tempo:Float,grainMs:Int,jitter:Float):Wav{
  var x=w.samples
  if(abs(tempo-1f)>.01f)x=resample(x,w.channels,tempo)
  if(abs(semitones)>.01f){val ratio=2.0.pow(semitones/12.0).toFloat();val old=x;x=resample(old,w.channels,ratio)}
  if(grainMs>0&&jitter>0f)x=granular(x,w.channels,max(w.channels,(w.sampleRate*grainMs/1000)*w.channels),jitter)
  return Wav(w.sampleRate,w.channels,x)
 }
 private fun resample(x:ShortArray,ch:Int,ratio:Float):ShortArray{
  val frames=x.size/ch;val outFrames=max(1,(frames/ratio).roundToInt());require(outFrames.toLong()*ch<Int.MAX_VALUE){"Файл слишком большой"}
  val out=ShortArray(outFrames*ch)
  for(f in 0 until outFrames){val p=f*ratio;val i=min(frames-1,p.toInt());val j=min(frames-1,i+1);val t=p-i;for(c in 0 until ch)out[f*ch+c]=((1-t)*x[i*ch+c]+t*x[j*ch+c]).roundToInt().coerceIn(-32768,32767).toShort()}
  return out
 }
 private fun granular(x:ShortArray,ch:Int,grain:Int,jitter:Float):ShortArray{
  val g=max(ch,grain-(grain%ch));if(x.size<g*2)return x
  val out=x.copyOf();val count=x.size/g
  for(i in 0 until count){if(Random.nextFloat()<jitter){val j=(i+Random.nextInt(-2,3)).coerceIn(0,count-1);val src=j*g;val dst=i*g;val n=min(g,min(x.size-src,out.size-dst));x.copyInto(out,dst,src,src+n)}}
  return out
 }
 fun write(w:Wav,out:OutputStream){val d=DataOutputStream(BufferedOutputStream(out));fun a(s:String)=d.write(s.toByteArray(Charsets.US_ASCII));fun l16(v:Int){d.writeByte(v);d.writeByte(v shr 8)};fun l32(v:Int){d.writeByte(v);d.writeByte(v shr 8);d.writeByte(v shr 16);d.writeByte(v shr 24)};val bytes=w.samples.size*2;a("RIFF");l32(36+bytes);a("WAVEfmt ");l32(16);l16(1);l16(w.channels);l32(w.sampleRate);l32(w.sampleRate*w.channels*2);l16(w.channels*2);l16(16);a("data");l32(bytes);w.samples.forEach{l16(it.toInt())};d.flush()}
}