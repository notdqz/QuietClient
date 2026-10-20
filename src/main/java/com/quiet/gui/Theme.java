package com.quiet.gui;

import com.quiet.config.QuietConfig;

public final class Theme {
	public int bgTop, bgBottom, shadowA, glossA;
	public int container, header, headerBorder, headerText;
	public int row, rowBorder, rowActive, rowActiveBorder, settingsBg;
	public int text, textActive, subtle, subtleActive, dim;
	public int trackOff, knobOff, trackOn, knobOn;
	public int checkBorder, checkOff, checkOn, checkMark;
	public int sliderTrack, sliderFill, sliderKnob;
	public int searchBg, searchBorder, searchText, searchHint, icon;
	public int panel, panelBorder, panelRowA, panelRowB, panelText, panelSub, toastOn, toastOff;

	public static final Theme DARK = new Theme();
	public static final Theme LIGHT = new Theme();

	static {
		Theme d = DARK;
		d.bgTop = 0x38000000; d.bgBottom = 0xA0000000; d.shadowA = 0x80; d.glossA = 0x1E;
		d.container = 0xC40E0F12; d.header = 0xFF2B2C30; d.headerBorder = 0x40FFFFFF; d.headerText = 0xFFFFFFFF;
		d.row = 0xFF1B1C20; d.rowBorder = 0x14FFFFFF; d.rowActive = 0xFF2D2E33; d.rowActiveBorder = 0x3CFFFFFF;
		d.settingsBg = 0xFF15161A;
		d.text = 0xFF8E9096; d.textActive = 0xFFFFFFFF; d.subtle = 0xFF74767C; d.subtleActive = 0xFFF0F0F2; d.dim = 0xFF4C4E54;
		d.trackOff = 0xFF3A3B40; d.knobOff = 0xFF7A7C82; d.trackOn = 0xFFB8BAC0; d.knobOn = 0xFFFFFFFF;
		d.checkBorder = 0x38FFFFFF; d.checkOff = 0xFF202125; d.checkOn = 0xFFD0D2D8; d.checkMark = 0xFF101114;
		d.sliderTrack = 0xFF3A3B40; d.sliderFill = 0xFFC8CAD0; d.sliderKnob = 0xFFFFFFFF;
		d.searchBg = 0xE61C1D21; d.searchBorder = 0x32FFFFFF; d.searchText = 0xFFFFFFFF; d.searchHint = 0xFF7C7E84; d.icon = 0xFFFFFFFF;
		d.panel = 0xF0131418; d.panelBorder = 0x30FFFFFF; d.panelRowA = 0xFF1A1B1F; d.panelRowB = 0xFF141519;
		d.panelText = 0xFFFFFFFF; d.panelSub = 0xFF8E9096; d.toastOn = 0xFF5A6468; d.toastOff = 0xFF8C9296;

		Theme l = LIGHT;
		l.bgTop = 0x30FFFFFF; l.bgBottom = 0x80E6E8EC; l.shadowA = 0x3C; l.glossA = 0x50;
		l.container = 0xCCF3F4F6; l.header = 0xFFFFFFFF; l.headerBorder = 0x34000000; l.headerText = 0xFF111214;
		l.row = 0xFFF6F7F9; l.rowBorder = 0x16000000; l.rowActive = 0xFFFFFFFF; l.rowActiveBorder = 0x3C000000;
		l.settingsBg = 0xFFEDEEF1;
		l.text = 0xFF7C7E84; l.textActive = 0xFF111214; l.subtle = 0xFF8A8C92; l.subtleActive = 0xFF222328; l.dim = 0xFFB4B6BC;
		l.trackOff = 0xFFD0D2D8; l.knobOff = 0xFFFFFFFF; l.trackOn = 0xFF2E3036; l.knobOn = 0xFFFFFFFF;
		l.checkBorder = 0x34000000; l.checkOff = 0xFFFFFFFF; l.checkOn = 0xFF2E3036; l.checkMark = 0xFFFFFFFF;
		l.sliderTrack = 0xFFD0D2D8; l.sliderFill = 0xFF2E3036; l.sliderKnob = 0xFF2E3036;
		l.searchBg = 0xF2FFFFFF; l.searchBorder = 0x32000000; l.searchText = 0xFF111214; l.searchHint = 0xFF8A8C92; l.icon = 0xFF111214;
		l.panel = 0xF5FFFFFF; l.panelBorder = 0x30000000; l.panelRowA = 0xFFF3F4F6; l.panelRowB = 0xFFFFFFFF;
		l.panelText = 0xFF111214; l.panelSub = 0xFF8A8C92; l.toastOn = 0xFF5A6468; l.toastOff = 0xFF8C9296;
	}

	public static Theme current() { return QuietConfig.darkMode ? DARK : LIGHT; }
}