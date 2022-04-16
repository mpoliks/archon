(

~hardwareInputs = 1;
~hardwareOutputs = 2;
~sr = 10;

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
	trig = Impulse.kr(~sr);

	SendReply.kr(trig, '/analysis',[
		peak,
		amp,
		freq[0].cpsmidi,
		freq[1],
		centroid,
		flat,
		rolloff

	],
		in);

}).add;

)

(

~analysisBus = Bus.audio(s, 1);
~inputGroup = Group.tail(s);
~analysisGroup = Group.tail(s);
~dsp = Analysis.new(~sr);


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


)

