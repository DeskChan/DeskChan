package info.deskchan.talking_system.speech_exchange;

public interface ICompatible {

    // Check how much (from 0 to 1) if other can be interpreted as this
    double checkCompatibility(ICompatible other);
}
