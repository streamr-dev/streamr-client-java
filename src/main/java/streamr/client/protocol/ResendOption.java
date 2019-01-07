package streamr.client.protocol;

public class ResendOption {

    public static String RESEND_ALL_KEY = "resend_all";
    public static String RESEND_LAST_KEY = "resend_last";
    public static String RESEND_FROM_KEY = "resend_from";

    private String key;
    private Object value;

    private ResendOption() {}

    private ResendOption(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public static ResendOption createNoResendOption() {
        return new ResendOption();
    }

    public static ResendOption createResendAllOption() {
        return new ResendOption(RESEND_ALL_KEY, true);
    }

    public static ResendOption createResendLastOption(int howMany) {
        return new ResendOption(RESEND_LAST_KEY, howMany);
    }

    public static ResendOption createResendFromOption(long fromOffset) {
        return new ResendOption(RESEND_FROM_KEY, fromOffset);
    }

    public boolean hasResendOption() {
        return key != null;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

}
