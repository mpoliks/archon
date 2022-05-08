(

~netAddr = NetAddr("127.0.0.1", 5005);
("OK: Listening on " + NetAddr.langPort.asString + ".").postln;

~hardwareInputs = 1;
~hardwareOutputs = 2;
~fr = 10;

ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;
s.meter;

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
	env = EnvGen.kr(Env.new([0,1,0],[atk, rel],[1, -1]),doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig * env);

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
	sig = HDVerb.ar(sig, predelay: predelay, decay: decay, lpf1: lpf1, lpf2: lpf2);
	Out.ar(out, sig);

}).add;

SynthDef(\freezeVerb, {
	|in, out, predelay, decay, lpf|
	var sig;

	sig = In.ar(in, 2);
	sig = FreezeVerb.ar(sig, predelay: predelay, decay: decay, lpf: lpf);
	Out.ar(out, sig);

}).add;

)

(
~analysisBus = Bus.audio(s, 1);
~reverbShortBus = Bus.audio(s,2);
~reverbMidBus = Bus.audio(s,2);
~reverbLongBus = Bus.audio(s,2);

~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~fxGroup = Group.tail(s);
~dryOutputGroup = Group.tail(s);
~dsp = Analysis.new(~fr, ~netAddr);


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
		\out, 0
	], ~fxGroup
);


~reverbMidSynth = Synth(
	\hdVerb, [
		\in, ~reverbMidBus,
		\decay, 3.0,
		\predelay, 0.1,
		\lpf1, 10000,
		\lpf2, 14000,
		\out, 0
	], ~fxGroup
);


~reverbLongSynth = Synth(
	\freezeVerb, [
		\in, ~reverbLongBus,
		\decay, 13,
		\predelay, 1.0,
		\lpf, 3000,
		\out, 0
	], ~fxGroup
);

)

(


OSCdef(
	\listener, {

	|msg|

	~dsp.envDetect(msg);

}, '/analysis');


OSCdef(
	\pythonlistener, {

	|msg|

	msg.postln;

}, '/superInterface');


OSCdef(
	\nodelistener, {

	|msg|

	var thishandler;

	"OK: Sending message to handler.".postln;
	thishandler = Handler.new;

	thishandler.msg_handler(msg[1..msg.size], s);


}, '/node');



)

