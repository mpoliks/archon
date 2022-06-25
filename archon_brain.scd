/*

The Brain is just a connection to the audio server and an OSC switchboard.
This is the file you need to run.
If you see errors, make sure you've moved the library folder to the extensions folder, eg.
'/Library/Application Support/SuperCollider/Extensions'.

*/
(

~netAddr = NetAddr("127.0.0.1", 5005); // Python address
("OK: Listening on " + NetAddr.langPort.asString ++ ".").postln;
// This is what SC is listening on, if you don't hear back from Python, make sure Python is listening on this port.

~detectionThreshold = -60.dbamp;

~hardwareInputs = 1; // number of mics connected
~hardwareOutputs = 16; // number of hardware outputs - right now this is configured for stereo
~fr = 10; // how frequently to sample input for descriptors
~outputChannel = 0; // remember: SuperCollider channel 0 = mixer channel 1
~numOutputs = 8;
~numPairs = ~numOutputs / 2;

//
~numVoices = 32;
~voiceCounter = 0;
~densityMax = 30;
~doneFlag = false;

~sampleSize = 0.5;
~bufList = List.new();

~targetOverride = false;
~specTarget = \sc;

// server settings, don't mess with these:
ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.options.memSize = 65536;
s.options.numBuffers = 4096;
s.reboot;
s.meter;
// s.dumpOSC(code:1); //for debugging

s.waitForBoot { // waits for server to boot to start defining server objects

SynthDef(\pulse, {

	var trig = Impulse.kr(0),

	env = EnvGen.kr(
			Env.new(
				[1, 0],
				[1]),
			doneAction:2);

	SendReply.kr(
			trig,
			'/pulse'
		);

}).add;

SynthDef(\playback, { // playback synth
	|rate = 1,
	atk = 0.1,
	rel = 0.4,
	cutoff = 500,
	pan = 0,
	buf,
	rq = 4.0,
	amp = 1.0,
	out|

	var sig = PlayBuf.ar(
		1,
		buf,
		BufRateScale.ir(buf) * rate,
		doneAction: 2),

	env = EnvGen.kr(
			Env.new(
				[0,1,0],
				[atk, rel],
				[1, -1]),
			doneAction:2);

	sig = BPF.ar(sig, cutoff);

	sig = Pan2.ar(sig, pan, amp);

	Out.ar(out, sig * env);

}).add;

SynthDef(\playgran, { // granular synth
		|out = 0,
		freq = 10,
		amp = 0.3,
		rate = 1.0,
		atk = 0.5,
		rel = 4,
		cutoff = 1500,
		buf,
		pan = 0|


    var env = EnvGen.kr(
			Env.new(
				[0,1,0],
				[atk, rel],
				[1, -1]),
			levelScale:amp,
			doneAction:2),


	fenv = EnvGen.kr(
		Env(
			[cutoff / 2, cutoff, cutoff / 2],
			[atk, rel],
			\sin, 1),
		levelScale: amp),

	sig = GrainBuf.ar(
				2,
				Impulse.kr(freq),
				0.2,
				buf,
				rate,
				LFNoise2.kr(0.1).range(0, 1),
				2,
				(pan * LFNoise2.kr(0.1).range(-1, 1)),
				-1);

	sig = BPF.ar(sig, fenv);

	Out.ar(out, sig * env)

}).add;

SynthDef(\analysisIn, { // simple patchcord

	|out = 0,
	in = 0|

	var micIn = SoundIn.ar(in, 1);

	Out.ar(out, micIn);

}).add;

SynthDef(\inputAnalysis, { // analysis synth

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

SynthDef(\hdVerb, { // nice sounding SC reverb
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

SynthDef(\freezeVerb, { // freeze reverb
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

SynthDef(\dryIO, { // compressor on the dry bus

	|out = 0,
	in = 0|

	var sig = In.ar(in, 2);

	sig = Compander.ar(sig,
			sig,
			thresh: 0.4,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.09);

	Out.ar(out, sig);

}).add;

SynthDef(\mixIO, { // compressor and limiter on the mix bus

	|out = 0,
	in = 0|

	var sig = In.ar(in, 2);

	sig = Compander.ar(sig,
			sig,
			thresh: 0.5,
			slopeBelow: 1.0,
			slopeAbove: 0.25,
			clampTime: 0.01,
			relaxTime: 0.01);

	sig = Limiter.ar(sig,
			level: 0.8,
			dur: 0.1);

	Out.ar(out, sig);

}).add;

s.sync;

~analysisBus = Bus.audio(s, 1);
// bus for analysis - if we're doing stereo inputs need to change this
~reverbShortBus = Array.fill(~numPairs,
		{
			Bus.audio(s, 2)
	});
~reverbMidBus = Array.fill(~numPairs,
		{
			Bus.audio(s, 2)
	});
~reverbLongBus = Array.fill(~numPairs,
		{
			Bus.audio(s, 2)
	});
~dryBus = Array.fill(~numPairs,
		{
			Bus.audio(s, 2)
	});
~mixBus = Array.fill(~numPairs,
		{
			Bus.audio(s, 2)
	});

~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~fxGroup = Group.tail(s);
~mixGroup = Group.tail(s);
// sets sequence for buses

~res = Buffer.read(s, "/Users/marek_orpheus/Desktop/ARCHON_db/B/4/2021-02-24_03-07_10evzcbq_1nckwb0l_sample7_0_500ms.wav");

~dsp = Analysis.new(~fr, ~netAddr, ~detectionThreshold); // initiate Analysis module
~voices = Array.fill(~numVoices, {
		Handler.new; // initialize Handler module
	});
~behavior = Behavior.new(); // initialize Behavior module

Pdef(\pulsar, Pbind(
			\instrument, \pulse,
			\dur, Pwhite(0.01, 10, inf)
	));

Synth(
	\analysisIn, [
		\out, ~analysisBus.index,
		\in, 0
	], ~inputGroup
);

Synth(
	\analysisIn, [
		\out, ~outputChannel + ~numOutputs,
		\in, 0
	], ~inputGroup
);

Synth(
	\inputAnalysis, [
		\in, ~analysisBus.index
	], ~analysisGroup
);

~reverbShortSynth = Array.fill(~numPairs, {
	|i|
	Synth(
		\hdVerb, [
			\in, ~reverbShortBus[i],
			\decay, 0.5,
			\predelay, 0.02,
			\lpf1, 10000,
			\lpf2, 14000,
			\out, ~mixBus[i]
		], ~fxGroup
	)
});

~reverbMidSynth = Array.fill(~numPairs, {
	|i|
	Synth(
		\hdVerb, [
			\in, ~reverbMidBus[i],
			\decay, 3.0,
			\predelay, 0.1,
			\lpf1, 10000,
			\lpf2, 14000,
			\out, ~mixBus[i]
		], ~fxGroup
	);
});

~reverbLongSynth = Array.fill(~numPairs, {
	|i|
	Synth(
		\freezeVerb, [
			\in, ~reverbLongBus[i],
			\decay, 13,
			\predelay, 1.0,
			\lpf, 3000,
			\out, ~mixBus[i]
		], ~fxGroup
	);
});

~drySynth = Array.fill(~numPairs, {
	|i|
	Synth(
		\dryIO, [
			\in, ~dryBus[i],
			\out, ~mixBus[i],
		], ~fxGroup
	);
});

~mixSynth = Array.fill(~numPairs, {
	|i|
	Synth(
		\mixIO, [
			\in, ~mixBus[i],
			\out, ~outputChannel + (2 * i)
		], ~mixGroup
	);
});

s.sync;

OSCdef(
	\listener, {

	|msg|

	~dsp.envDetect(msg); // sends analysis to the Analysis module

}, '/analysis');


OSCdef(
	\pythonlistener, {

	|msg|

	"OK: Python ACK Incoming".postln;
	msg[1].postln; // just waits for Python acknowledgement

}, '/superInterface');


OSCdef(
	\pulselistener, {

	if (s.avgCPU > 60, {
			"WARN: CPU High, Taking a Short Break".postln;
		},
		{
			if (~doneFlag == true, {
			"WARN: Piece Done!".postln;
				},
				{
					~voiceCounter = (~voiceCounter + 1) % ~numVoices;
					("OK: Selecting Voice" + ~voiceCounter.asString ++ ".").postln;
					if (~bufList.size > 0, {
						~voices[~voiceCounter].msg_handler(~bufList.scramble[0], s);
						// sends Python output to Handler module
					});
				});
			});

}, '/pulse');


OSCdef(
	\nodelistener, {

	|msg|

	~bufList.add(msg[1..msg.size]);

	if (~bufList.size > 3,
		{
			~bufList.removeAt(0)
		});

}, '/node');


OSCdef(
	\behaviorlistener, {

	|msg|

	var args = ~behavior.receiver(msg),
	win = args[1].at(\windowsize).asFloat.linlin(0, 15, 1, 5, clip: \minmax),
	dense = args[1].at(\density).asFloat.linlin(0, 25, 0.01, 25, clip: \minmax),
	dur = (win * dense).linlin(0, 15, 0.01, 15, clip: \minmax);
	("density: " + dense.asString).postln;
	("windowsize: " + win.asString).postln;

	Pdef(\pulsar,
		Pbind(
			\instrument, \pulse,
			\dur, Pwhite((dur / 3), (dur + 8))
		)
	);

	4.do({
		|c|
		~voices[((~voiceCounter + c + 1) % ~numVoices)]
		.settings(
			args); // sets Handler settings with the Behavior module
	});

}, '/behavior');

"OK: Ready!".postln;

Pdef(\pulsar).play;

Window.closeAll;

s.meter (~hardwareInputs, ~numOutputs + 1);

~w = Window("GUI Controls", Rect(0,-100,360,200))
	.front
	.alwaysOnTop_(true);
	~w.view.decorator_(
		FlowLayout(
			~w.bounds,
			10@10,
			10@10));


b = Button(~w, Rect(20, 20, 340, 30))
	.states_([
		["Engage SC Override", Color.black, Color.yellow],
	])
	.action_({
		|state|
		~targetOverride = true;
		switch(state.value,
				0, {
					~specTarget = \sc;
				},
		);
	});

b = Button(~w, Rect(20, 20, 340, 30))
	.states_([
		["Engage ST Override", Color.black, Color.red],
	])
	.action_({
		|state|
		~targetOverride = true;
		switch(state.value,
				0, {
					~specTarget = \st;
				},
		);
	});

b = Button(~w, Rect(20, 20, 340, 30))
	.states_([
		["Engage SO Override", Color.black, Color.yellow],
	])
	.action_({
		|state|
		~targetOverride = true;
		switch(state.value,
				0, {
					~specTarget = \so;
				},
		);
	});

b = Button(~w, Rect(20, 20, 340, 30))
	.states_([
		["Engage GP Override", Color.black, Color.red],
	])
	.action_({
		|state|
		~targetOverride = true;
		switch(state.value,
				0, {
					~specTarget = \gp;
				},
		);
	});


s.record(numChannels:(~numOutputs + 1)); // optional to record output

}

)