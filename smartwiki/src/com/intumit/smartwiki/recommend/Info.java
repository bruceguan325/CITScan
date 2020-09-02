package com.intumit.smartwiki.recommend;

class Info {

    private Info() {
    }
    public Info(char ch) {
        this.ch = ch;
    }

    public static final Info EMPTY = new Info();

    public char ch;

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Info)) {
            return false;
        }
        Info that = (Info) obj;
        return this.ch == that.ch;
    }
}
