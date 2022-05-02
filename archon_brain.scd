(

~netAddr = NetAddr("127.0.0.1", 5005);

~hardwareInputs = 1;
~hardwareOutputs = 2;
~fr = 10;

ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;
s.meter;

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

SynthDef(\playback, {

	|out = 0,
	attack,
	release,
	buffer,
	loop,
	envTrig,
	done,
	amp = 1.0|

	var env, sig;

	env = EnvGen.kr(
		Env.asr(attack, amp, release),
		envTrig,
		doneAction: 2
	);

	sig = PlayBuf.ar(
			1,
			buffer,
			rate: 1,
			loop: loop,
			doneAction: 2);

	sig = (sig * env * amp);
	Out.ar(out, sig);

}).add;

)

(

~analysisBus = Bus.audio(s, 1);
~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~outputGroup = Group.tail(s);
~dsp = Analysis.new(~fr, ~netAddr);


Synth(
	\audioIn, [
		\out, ~analysisBus.index,
		\in, 0
	],
		~inputGroup
);


Synth(
	\inputAnalysis, [
		\in, ~analysisBus.index
	],
	~analysisGroup
);


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

	var buf;

	buf = Buffer.read(s, msg[1], action: {
		Synth(
			\playback,
				[
					\out, 0,
					\buffer, buf,
					\envTrig, 1,
					\loop, 0
				],
				~outputGroup
			)
		});


}, '/node');


)
