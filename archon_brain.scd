(

~netAddr = NetAddr("127.0.0.1", 5005);
("OK: Listening on " + NetAddr.langPort.asString ++ ".").postln;

~hardwareInputs = 1;
~hardwareOutputs = 2;
~fr = 10;

ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;
s.meter;

s.waitForBoot {

SynthDef(\playback, {
	|rate = 1,
	atk = 0.1,
	rel = 0.4,
	pan = 0,
	buf,
	amp = 1.0,
	out|

	var sig, env;

	sig = PlayBuf.ar(
		1,
		buf,
		BufRateScale.ir(buf) * rate,
		doneAction: 2);
	env = EnvGen.kr(
			Env.new(
				[0,1,0],
				[atk, rel],
				[1, -1]),
			doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig * env);

}).add;

SynthDef(\playgran, {
		|out,
		gate = 1,
		freq = 10,
		amp = 1,
		rate = 1.0,
		sndbuf,
		pan = 0,
		envbuf|

    var env, freqdev;

    env = EnvGen.kr(
        Env([0, 1, 0], [1, 1], \sin, 1),
        gate,
        levelScale: amp,
        doneAction: Done.freeSelf);

    Out.ar(out,
        GrainBuf.ar(
				2,
				Impulse.kr(freq),
				0.1,
				sndbuf,
				rate,
				LFNoise2.kr(0.1).range(0, 1),
				2,
				(pan * LFNoise2.kr(0.1).range(-1, 1)),
				envbuf) * env)
}).add;

SynthDef(\audioIn, {

	|out = 0,
	in = 0|

	var micIn = SoundIn.ar(in, 1);

	Out.ar(out, micIn)

}).add;

SynthDef(\inputAnalysis, {

	|in = 0|

	var input = In.ar(in),
	peak = Amplitude.ar(input),
	amp = RMS.ar(input),
	freq = Pitch.kr(input),
	chain = FFT(LocalBuf(2048), input),
    centroid = SpecCentroid.kr(chain),
	flat = SpecFlatness.kr(chain),
	rolloff = SpecPcile.kr(chain, 0.5),
	trig = Impulse.kr(~fr);

	SendReply.kr(trig,
		'/analysis',[
			peak,
			amp,
			freq[0],
			freq[1],
			centroid,
			flat,
			rolloff
			],
		in);

}).add;

SynthDef(\hdVerb, {
	|in, out, predelay, decay, lpf1, lpf2|
	var sig;

	sig = In.ar(in, 2);
	sig = Compander.ar(sig,
			sig,
			thresh: 0.5,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.09);
	sig = HDVerb.ar(sig,
			predelay: predelay,
			decay: decay,
			lpf1: lpf1,
			lpf2: lpf2);
	Out.ar(out, sig);

}).add;

SynthDef(\freezeVerb, {
	|in, out, predelay, decay, lpf|
	var sig;

	sig = In.ar(in, 2);
	sig = Compander.ar(sig,
			sig,
			thresh: 0.5,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.09);
	sig = FreezeVerb.ar(sig,
			predelay: predelay,
			decay: decay,
			lpf: lpf);
	Out.ar(out, sig);

}).add;

SynthDef(\dryIO, {

	|out = 0,
	in = 0|

	var sig = In.ar(in, 2);

	sig = Compander.ar(sig,
			sig,
			thresh: 0.5,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.09);

	Out.ar(out, sig);

}).add;

SynthDef(\mixIO, {

	|out = 0,
	in = 0|

	var sig = In.ar(in, 2);

	sig = Compander.ar(sig,
			sig,
			thresh: 0.7,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.01);

	sig = Limiter.ar(sig,
			level: 1.0,
			dur: 0.1);

	Out.ar(out, sig);

}).add;

s.sync;

~analysisBus = Bus.audio(s, 1);
~reverbShortBus = Bus.audio(s,2);
~reverbMidBus = Bus.audio(s,2);
~reverbLongBus = Bus.audio(s,2);
~dryBus = Bus.audio(s, 1);
~mixBus = Bus.audio(s, 1);

~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~fxGroup = Group.tail(s);
~mixGroup = Group.tail(s);
~dsp = Analysis.new(~fr, ~netAddr);
~handler = Handler.new;
~behavior = Behavior.new();

Synth(
	\audioIn, [
		\out, ~analysisBus.index,
		\in, 0
	], ~inputGroup
);

Synth(
	\inputAnalysis, [
		\in, ~analysisBus.index
	], ~analysisGroup
);

~reverbShortSynth = Synth(
	\hdVerb, [
		\in, ~reverbShortBus,
		\decay, 0.5,
		\predelay, 0.02,
		\lpf1, 10000,
		\lpf2, 14000,
		\out, ~mixBus
	], ~fxGroup
);

~reverbMidSynth = Synth(
	\hdVerb, [
		\in, ~reverbMidBus,
		\decay, 3.0,
		\predelay, 0.1,
		\lpf1, 10000,
		\lpf2, 14000,
		\out, ~mixBus
	], ~fxGroup
);

~reverbLongSynth = Synth(
	\freezeVerb, [
		\in, ~reverbLongBus,
		\decay, 13,
		\predelay, 1.0,
		\lpf, 3000,
		\out, ~mixBus
	], ~fxGroup
);

~drySynth = Synth(
	\dryIO, [
		\in, ~dryBus,
		\out, ~mixBus,
	], ~fxGroup
);

~mixSynth = Synth(
	\mixIO, [
		\in, ~mixBus,
		\out, 0
	], ~mixGroup

);

s.sync;

OSCdef(
	\listener, {

	|msg|

	~dsp.envDetect(msg);

}, '/analysis');


OSCdef(
	\pythonlistener, {

	|msg|

	"OK: ACK from Python".postln;

}, '/superInterface');


OSCdef(
	\nodelistener, {

	|msg|

	~handler.msg_handler(msg[1..msg.size], s);

}, '/node');


OSCdef(
	\behaviorlistener, {

	|msg|

	~behavior.receiver(msg);

}, '/behavior');

"OK: Ready!".postln;

//s.record(numChannels:2);

}

)

~behavior.get(onsetTime)

~handler.switchstate("granplay", 1.0)