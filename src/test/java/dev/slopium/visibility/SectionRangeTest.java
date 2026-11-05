package dev.slopium.visibility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SectionRangeTest {
    @Test void minimumIsStrictlyBelowViewDistance(){for(int r=1;r<=64;r++)for(int n=-2;n<=100;n++){int m=SectionRange.minimum(n,r);assertTrue(m>=0&&m<r);}}
    @Test void zeroStillProtectsCameraSection(){assertFalse(SectionRange.eligible(0,0,0,0,0,0,0,12));assertTrue(SectionRange.eligible(0,1,0,0,0,0,0,12));}
    @Test void minimumAndViewBoundariesAreInclusive(){assertFalse(SectionRange.eligible(2,-2,2,0,0,0,2,12));assertTrue(SectionRange.eligible(0,-12,0,0,0,0,2,12));assertFalse(SectionRange.eligible(0,13,0,0,0,0,2,12));}
    @Test void negativeWorldCoordinatesAndLoweredRange(){assertFalse(SectionRange.eligible(-3,-5,-7,-4,-4,-6,1,5));assertEquals(4,SectionRange.minimum(20,5));}
    @Test void coordinateDifferenceDoesNotOverflow(){assertFalse(SectionRange.eligible(Integer.MAX_VALUE,0,0,Integer.MIN_VALUE,0,0,0,32));}
}
