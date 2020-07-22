package kahdeg.sound;

import com.cburch.logisim.util.StringGetter;

public class SimpleStringGetter implements StringGetter {
    private String str;

    public SimpleStringGetter(String str) {
        this.str = str;
    }

    public String get() {
        return str;
    }

    @Override
    public String toString() {
        return str;
    }
}
