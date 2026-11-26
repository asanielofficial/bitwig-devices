component asd_vibrato
{
	audio_inport in;
	audio_inport f32 rate = 0.5;
	audio_inport f32 depth = 0.25;
	audio_inport f32 stereo = 0.0;
	audio_inport f32 mix = 1.0;
	audio_inport f32 beats_per_frame = 0.0;
	value_inport i32 shape;
	value_inport i32 timebase;
	value_inport bool retrigger;
	event_inport NoteEvent note_in
	{
		if (note_in.type == note_in.NOTE_ON)
		{
			last_key = note_in.key;
			if (retrigger)
				pending_reset = true;
		}
	}
	audio_outport out;
	audio_outport f32<4> display;
	audio_outport f32<4> display_hold;
	const i32 MAX_FRAMES = EC.samplerate / 10 + 8;
	f32<>[MAX_FRAMES] delay_line;
	i32 write_position = 0;
	f32 phase = 0.0;
	f32 hold_z = 0.0;
	f32 hold_a = 0.0;
	f32 hold_b = 0.0;
	f32 cum_sh = 0.0;
	f32 cum_sl = 0.0;
	f32 target_kappa = 0.0;
	f32 kappa = 0.0;
	f32 offset_smooth = 0.0;
	f32 mix_smooth = 1.0;
	i32 seed = 22222;
	i32 last_key = 69;
	bool pending_reset = false;
	f32 next_random()
	{
		seed = ((seed).i64 * 48271 % 2147483647).i32;
		return (seed).f32 / 2147483647.0 * 2.0 - 1.0;
	}
	f32 beats_for(i32 i)
	{
		if (i == 2)
			return 4.0;
		if (i == 3)
			return 2.0;
		if (i == 4)
			return 1.0;
		if (i == 5)
			return 0.5;
		if (i == 6)
			return 0.25;
		if (i == 7)
			return 0.125;
		if (i == 8)
			return 3.0;
		if (i == 9)
			return 1.5;
		if (i == 10)
			return 0.75;
		if (i == 11)
			return 0.375;
		if (i == 12)
			return 0.1875;
		if (i == 13)
			return 1.3333333;
		if (i == 14)
			return 0.6666667;
		if (i == 15)
			return 0.3333333;
		if (i == 16)
			return 0.1666667;
		return 0.0833333;
	}
	i32 wrapIndex(i32 index)
	{
		if (index < 0)
			return index + MAX_FRAMES;
		if (index >= MAX_FRAMES)
			return index - MAX_FRAMES;
		return index;
	}
	f32<> readDelay(f32 delay)
	{
		f32 position = (write_position).f32 - delay;
		f32 whole = floor(position);
		i32 base = (whole).i32;
		f32 frac = position - whole;
		f32<> ym1 = delay_line[wrapIndex(base - 1)];
		f32<> y0 = delay_line[wrapIndex(base)];
		f32<> y1 = delay_line[wrapIndex(base + 1)];
		f32<> y2 = delay_line[wrapIndex(base + 2)];
		f32<> c1 = 0.5 * (y1 - ym1);
		f32<> c2 = ym1 - 2.5 * y0 + 2.0 * y1 - 0.5 * y2;
		f32<> c3 = 0.5 * (y2 - ym1) + 1.5 * (y0 - y1);
		return ((c3 * frac + c2) * frac + c1) * frac + y0;
	}
	f32 zpeak(i32 which)
	{
		if (which == 0)
			return 0.15915494;
		if (which == 1)
			return 0.125;
		if (which == 2)
			return 0.25;
		if (which == 3)
			return 0.16666667;
		if (which == 4)
			return 0.16666667;
		return 0.8;
	}
	f32 smoothIntegral(f32 s0, f32 s1, f32 t)
	{
		return s0 * t + (s1 - s0) * (t * t * t - 0.5 * t * t * t * t);
	}
	f32 zAt(i32 which, f32 ph, f32 lag)
	{
		f32 p = ph - lag;
		if (which == 5)
		{
			if (p >= 0.0)
				return cum_sh + hold_b * p;
			return cum_sh + hold_a * p;
		}
		if (which == 6)
		{
			if (p >= 0.0)
				return cum_sl + smoothIntegral(hold_a, hold_b, p);
			return cum_sl - 0.5 * (hold_z + hold_a) + smoothIntegral(hold_z, hold_a, p + 1.0);
		}
		if (p < 0.0)
			p = p + 1.0;
		if (which == 0)
			return -cos(6.2831853 * p) / 6.2831853;
		if (which == 1)
		{
			if (p < 0.25)
				return 2.0 * p * p - 0.125;
			if (p < 0.75)
				return 2.0 * p - 2.0 * p * p - 0.375;
			return 2.0 * (p - 1.0) * (p - 1.0) - 0.125;
		}
		if (which == 2)
		{
			if (p < 0.5)
				return p - 0.25;
			return 0.75 - p;
		}
		if (which == 3)
			return p * p - p + 0.16666667;
		return p - p * p - 0.16666667;
	}
	process
	{
		f32<> dry = in;
		f32 sr = (EC.samplerate).f32;
		f32 r = fmax(0.0, fmin(1.0, rate));
		f32 multiplier = 0.02 * pow(2500.0, r);
		f32 cycles_per_frame = 0.0;
		if (timebase == 0)
			cycles_per_frame = multiplier / sr;
		else
			if (timebase == 1)
				cycles_per_frame = multiplier * 1000.0 / sr;
			else
				if (timebase == 18)
					cycles_per_frame = 440.0 * pow(2.0, ((last_key).f32 - 69.0) / 12.0) * multiplier / sr;
				else
					if (timebase == 19)
						cycles_per_frame = 0.0;
					else
						cycles_per_frame = beats_per_frame * multiplier / beats_for(timebase);
		f32 knob_smoothing = 1.0 / fmax(1.0, 0.03 * sr);
		if (pending_reset)
		{
			if (timebase != 19)
				phase = 0.0;
			pending_reset = false;
		}
		i32 which = shape;
		if (timebase == 19)
			phase = phase + (fmin(0.9999, r) - phase) * knob_smoothing;
		else
			phase = phase + cycles_per_frame;
		if (phase >= 1.0)
		{
			phase = phase - floor(phase);
			cum_sh = cum_sh + hold_b;
			cum_sl = cum_sl + 0.5 * (hold_a + hold_b);
			hold_z = hold_a;
			hold_a = hold_b;
			f32 lo = -0.6 - cum_sh;
			f32 hi = 0.6 - cum_sh;
			if (which == 6)
			{
				lo = 2.0 * (-0.6 - cum_sl) - hold_a;
				hi = 2.0 * (0.6 - cum_sl) - hold_a;
			}
			hold_b = fmax(-1.0, fmin(1.0, fmax(lo, fmin(hi, next_random()))));
		}
		f32 amount = fmax(0.0, fmin(1.0, depth));
		f32 ratio = pow(2.0, 100.0 * amount / 1200.0) - 1.0;
		f32 zp = zpeak(which);
		if (timebase == 19)
			target_kappa = fmin(0.03 * sr / zp, ratio * sr);
		else
			if (cycles_per_frame > 0.0000001)
				target_kappa = fmin(0.03 * sr / zp, ratio / cycles_per_frame);
		kappa = kappa + (target_kappa - kappa) / fmax(1.0, 0.08 * sr);
		offset_smooth = offset_smooth + (0.5 * fmax(0.0, fmin(1.0, stereo)) - offset_smooth) * knob_smoothing;
		f32 offset = offset_smooth;
		f32 swing = 2.0 * kappa * zp;
		f32 delay_left = fmax(2.0, fmin(2.0 + swing, 2.0 + kappa * (zp - zAt(which, phase, 0.0))));
		f32 delay_right = fmax(2.0, fmin(2.0 + swing, 2.0 + kappa * (zp - zAt(which, phase, offset))));
		delay_line[write_position] = dry;
		f32<> left_read = readDelay(delay_left);
		f32<> right_read = readDelay(delay_right);
		f32<> wet = <left_read[0], right_read[1]>;
		mix_smooth = mix_smooth + (fmax(0.0, fmin(1.0, mix)) - mix_smooth) * knob_smoothing;
		f32 m = mix_smooth;
		out = dry * (1.0 - m) + wet * m;
		display = <(which).f32, amount, phase, offset>;
		display_hold = <hold_z, hold_a, hold_b, 0.0>;
		write_position = wrapIndex(write_position + 1);
	}
}
