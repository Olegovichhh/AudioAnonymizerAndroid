package com.olegovichhh.audioanonymizer

import java.io.*
import kotlin.math.*
import kotlin.random.Random

object WavDsp {
    data class Wav(val sampleRate:Int,val channels:Int,val samples:ShortArray)

    fun read(input:InputStream):Wav {
        val d=DataInputStream(BufferedInputStream(input))
        fun ascii(n:Int)=ByteArray(n).also{d.readFully(it)}.toString(Charsets.US_ASCII)
        fun le16():Int { val a=d.read(); val b=d.read(); return a or (b shl 8) }
        fun le32():Int { val a=d.read();val b=d.read();val c=d.read();val e=d.read();return a or(b shl 8)or(c shl 16)or(e shl 24) }
        require(ascii(4)=="RIFF"); le32(); require(ascii(4)=="WAVE")
        var rate=0;var ch=0;var bits=0;var pcm=ByteArray(0)
        while(d.available()>0){
            val id=ascii(4); val size=le32()
            when(id){
                "fmt "->{ val format=le16();ch=le16();rate=le32();le32();le16();bits=le16(); if(size>16)d.skipBytes(size-16);require(format==1&&bits==16){"Only PCM 16-bit WAV is supported"} }
                "data"->{pcm=ByteArray(size);d.readFully(pcm)}
                else->d.skipBytes(size)
            }
        }
        require(rate>0&&ch in 1..2&&pcm.isNotEmpty())
        val s=ShortArray(pcm.size/2){i->((pcm[i*2].toInt() and 255) or (pcm[i*2+1].toInt() shl 8)).toShort()}
        return Wav(rate,ch,s)
    }
    fun process(w:Wav,semitones:Float,tempo:Float,grainMs:Int,jitter:Float):Wav{
        var x=w.samples
        if(abs(tempo-1f)>.01f)x=resample(x,w.channels,tempo)
        if(abs(semitones)>.01f){ val ratio=2.0.pow(semitones/12.0).toFloat(); x=resample(x,w.channels,ratio) }
        if(grainMs>0)x=granular(x,w.channels,max(w.channels,(w.sampleRate*grainMs/1000)*w.channels),jitter)
        return Wav(w.sampleRate,w.channels,x)
    }
    private fun resample(x:ShortArray,ch:Int,ratio:Float):ShortArray{
        val frames=x.size/ch;val outFrames=max(1,(frames/ratio).roundToInt());val out=ShortArray(outFrames*ch)
        for(f in 0 until outFrames){val p=f*ratio;val i=min(frames-1,p.toInt());val j=min(frames-1,i+1);val t=p-i
            for(c in 0 until ch)out[f*ch+c]=((1-t)*x[i*ch+c]+t*x[j*ch+c]).roundToInt().coerceIn(-32768,32767).toShort()}
        return out
    }
    private fun granular(x:ShortArray,ch:Int,grain:Int,jitter:Float):ShortArray{
        if(x.size<grain*2)return x
        val chunks=x.toList().chunked(grain).map{it.toShortArray()}.toMutableList()
        for(i in 0 until chunks.size-1)if(Random.nextFloat()<jitter){val j=(i+Random.nextInt(-2,3)).coerceIn(0,chunks.lastIndex);val t=chunks[i];chunks[i]=chunks[j];chunks[j]=t}
        return ShortArray(chunks.sumOf{it.size}).also{o->var p=0;chunks.forEach{g->g.copyInto(o,p);p+=g.size}}
    }
    fun write(w:Wav,out:OutputStream){
        val d=DataOutputStream(BufferedOutputStream(out));fun a(s:String)=d.write(s.toByteArray(Charsets.US_ASCII))
        fun l16(v:Int){d.writeByte(v);d.writeByte(v shr 8)};fun l32(v:Int){d.writeByte(v);d.writeByte(v shr 8);d.writeByte(v shr 16);d.writeByte(v shr 24)}
        val bytes=w.samples.size*2;a("RIFF");l32(36+bytes);a("WAVEfmt ");l32(16);l16(1);l16(w.channels);l32(w.sampleRate);l32(w.sampleRate*w.channels*2);l16(w.channels*2);l16(16);a("data");l32(bytes)
        w.samples.forEach{l16(it.toInt())};d.flush()
    }
}
