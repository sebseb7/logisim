package kahdeg.test;

import java.io.IOException;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.util.ZipClassLoader;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;

import java.util.Arrays;
import java.util.List;

// Referenced classes of package immibis.buzzer:
//            BuzzerFactory, ComplexBuzzerFactory

public class library extends com.cburch.logisim.tools.Library
{

    private List<AddTool> tools;

    public library(){
		tools = Arrays.asList(new AddTool[] {
                new AddTool(new kahdeg.sound.sound()),
        });
		System.out.println("Hello World");
    }

    public List getTools()
    {
        return tools;
    }

    public String getDisplayName()
    {
        return "kahdeg";
    }
	
	public String getName() {
		return "kahdeg";
	}

	@Override
	public boolean removeLibrary(String name) { return false; }
}
