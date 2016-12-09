package config;

/**
 * Created by jacobhong on 12/8/16.
 */
public class QnaResponse
{
    private boolean ok = true;

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}
