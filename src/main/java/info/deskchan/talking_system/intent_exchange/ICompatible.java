package info.deskchan.talking_system.intent_exchange;

public interface ICompatible {

    // Check how much (from 0 to 1) if other can be interpreted as this
    double checkCompatibility(ICompatible other);
}
