package com.olegovichhh.audioanonymizer
import java.io.*
import kotlin.math.*
import kotlin.random.Random
object WavDsp {
 data class Wav(val sampleRate:Int,val channels:Int,val samples:ShortArray)
 fun process(w:Wav,semitones:Float,tempo:Float,grainMs:Int,jitter:Float):Wav{
  var x=w.samples
  if(abs(semitones)>0.01f) x=pitchShiftPreserveDuration(x,w.channels,semitones)
  if(abs(tempo-1f)>0.01f) x=timeStretch(x,w.channels,tempo)
  if(grainMs>0&&jitter>0f)x=granular(x,w.channels,max(w.channels,(w.sampleRate*grainMs/1000)*w.channels),jitter)
  return Wav(w.sampleRate,w.channels,x)
 }
 private fun pitchShiftPreserveDuration(input:ShortArray,ch:Int,semi:Float):ShortArray{
  val ratio=2.0.pow(semi/12.0).toFloat()
  val pitched=resample(input,ch,ratio)
  return stretchToFrames(pitched,ch,input.size/ch)
 }
 private fun timeStretch(input:ShortArray,ch:Int,tempo:Float):ShortArray{
  val target=max(1,((input.size/ch)/tempo).roundToInt())
  return stretchToFrames(input,ch,target)
 }
 private fun stretchToFrames(input:ShortArray,ch:Int,targetFrames:Int):ShortArray{
  val frames=input.size/ch;if(frames<4)return input.copyOf()
  val window=min(2048,max(256,frames/20));val hopIn=max(64,window/4)
  val ratio=targetFrames.toDouble()/frames;val hopOut=max(1,(hopIn*ratio).roundToInt())
  val out=FloatArray((targetFrames+window+2)*ch);val weight=FloatArray(targetFrames+window+2)
  var inPos=0;var outPos=0
  while(inPos+window<frames&&outPos+window<weight.size){
   for(n in 0 until window){val win=(0.5-0.5*cos(2.0*Math.PI*n/(window-1))).toFloat();val of=outPos+n;weight[of]+=win;for(c in 0 until ch)out[of*ch+c]+=input[(inPos+n)*ch+c]*win}
   inPos+=hopIn;outPos+=hopOut
  }
  return ShortArray(targetFrames*ch){i->val f=i/ch;val w=if(weight[f]>0.0001f)weight[f] else 1f;(out[i]/w).roundToInt().coerceIn(-32768,32767).toShort()}
 }
 private fun resample(input:ShortArray,ch:Int,ratio:Float):ShortArray{
  val frames=input.size/ch;val outFrames=max(1,(frames/ratio).roundToInt());val out=ShortArray(outFrames*ch)
  for(f in 0 until outFrames){val p=f*ratio;val i=min(frames-1,p.toInt());val j=min(frames-1,i+1);val t=p-i;for(c in 0 until ch){val v=(1f-t)*input[i*ch+c]+t*input[j*ch+c];out[f*ch+c]=v.roundToInt().coerceIn(-32768,32767).toShort()}}
  return out
 }
 private fun granular(input:ShortArray,ch:Int,grain:Int,jitter:Float):ShortArray{
  val g=max(ch,grain-grain%ch);if(input.size<g*2)return input;val out=input.copyOf();val count=input.size/g
  for(i in 0 until count)if(Random.nextFloat()<jitter){val j=(i+Random.nextInt(-2,3)).coerceIn(0,count-1);val src=j*g;val dst=i*g;val n=min(g,min(input.size-src,out.size-dst));input.copyInto(out,dst,src,src+n)}
  return out
 }
 fun write(w:Wav,stream:OutputStream){val out=DataOutputStream(BufferedOutputStream(stream));fun a(s:String)=out.write(s.toByteArray(Charsets.US_ASCII));fun l16(v:Int){out.writeByte(v);out.writeByte(v shr 8)};fun l32(v:Int){out.writeByte(v);out.writeByte(v shr 8);out.writeByte(v shr 16);out.writeByte(v shr 24)};val bytes=w.samples.size*2;a("RIFF");l32(36+bytes);a("WAVE");a("fmt ");l32(16);l16(1);l16(w.channels);l32(w.sampleRate);l32(w.sampleRate*w.channels*2);l16(w.channels*2);l16(16);a("data");l32(bytes);for(s in w.samples)l16(s.toInt());out.flush()}
}