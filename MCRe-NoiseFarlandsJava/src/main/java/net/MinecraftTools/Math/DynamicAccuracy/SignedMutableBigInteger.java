package net.MinecraftTools.Math.DynamicAccuracy;

class SignedMutableBigInteger extends MutableBigInteger {

    int sign = 1;

    SignedMutableBigInteger() {
        super();
    }

    SignedMutableBigInteger(int val) {
        super(val);
    }

    SignedMutableBigInteger(MutableBigInteger val) {
        super(val);
    }

    void signedAdd(SignedMutableBigInteger addend) {
        if (sign == addend.sign)
            add(addend);
        else
            sign = sign * subtract(addend);
    }

    void signedAdd(MutableBigInteger addend) {
        if (sign == 1)
            add(addend);
        else
            sign = sign * subtract(addend);
    }

    void signedSubtract(SignedMutableBigInteger addend) {
        if (sign == addend.sign)
            sign = sign * subtract(addend);
        else
            add(addend);
    }

    void signedSubtract(MutableBigInteger addend) {
        if (sign == 1)
            sign = sign * subtract(addend);
        else
            add(addend);
        if (intLen == 0)
            sign = 1;
    }

    public String toString() {
        return this.toBigInteger(sign).toString();
    }

}
