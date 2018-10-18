package com.bfh.logisim.library;
import static com.cburch.logisim.std.Strings.S;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class BFHPraktika extends Library {
	private static FactoryDescription[] DESCRIPTIONS = {
		new FactoryDescription("Dynamic_Clock_Control",
		         S.getter("DynamicClockControl"), "",
		         "DynamicClock"), 
		new FactoryDescription("Binairy_to_BCD_converter",
		         S.getter("Bin2BCD"), "",
		         "bin2bcd"), 
		new FactoryDescription("BCD_to_7_Segment_decoder",
		         S.getter("BCD2SevenSegment"), "",
		         "bcd2sevenseg"),
		new FactoryDescription("Hex_to_7_Segment_decoder",
		         S.getter("Hex2SevenSegment"), "",
		         "hex2sevenseg"),
        };

private List<Tool> tools = null;

public BFHPraktika() {
}

@Override
public String getDisplayName() {
	return S.get("BFH mega functions");
}

@Override
public String getName() {
	return "BFH-Praktika";
}

@Override
public List<Tool> getTools() {
	if (tools == null) {
		tools = FactoryDescription.getTools(BFHPraktika.class, DESCRIPTIONS);
	}
	return tools;
}

}
