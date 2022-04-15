(

~hardwareInputs = 1;
~hardwareOutputs = 2;
ServerOptions.devices;
Server.default.options.numInputBusChannels = ~hardwareInputs;
Server.default.options.numOutputBusChannels = ~hardwareOutputs;
s.options;
s.reboot;

SynthDef(\audioIn, {

	|out = 0,
	in = 0|

	var micIn;
	micIn = SoundIn.ar(in, 1);
	Out.ar(out, micIn)

}).add;


SynthDef(\inputAnalysis, {

	|in = 0|

	var input = In.ar(in),
	amp = RMS.ar(input).ampdb,
	freq = Pitch.kr(input),
	chain = FFT(LocalBuf(2048), input),
    centroid = SpecCentroid.kr(chain),
	flat = SpecFlatness.kr(chain),
	rolloff = SpecPcile.kr(chain, 0.5),

	trig = Impulse.kr(1);

	SendReply.kr(trig, '/analysis',[
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


OSCdef(\listener, {

	|msg|

	msg.postln;

}, '/analysis');
)