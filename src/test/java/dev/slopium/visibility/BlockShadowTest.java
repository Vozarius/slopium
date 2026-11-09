package dev.slopium.visibility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlockShadowTest {
    private boolean behind(double ey,double targetMinY,double targetMaxY){
        return BlockShadow.covers(1,-1,-1,1.1,1,1, 4,targetMinY,-.2,5,targetMaxY,.2, 0,ey,0);
    }
    @Test void closedWallProvesAllCorners(){assertTrue(behind(0,-.2,.2));}
    @Test void walkingPastCornerRevealsModel(){assertFalse(behind(2,-.2,.2));}
    @Test void partialModelMustStayVisible(){assertFalse(behind(0,-.2,6));}
    @Test void touchingOrInsideBlockerNeverHides(){
        assertFalse(BlockShadow.covers(1,-1,-1,2,1,1,2,-.2,-.2,3,.2,.2,0,0,0));
        assertFalse(BlockShadow.covers(1,-1,-1,2,1,1,4,-.2,-.2,5,.2,.2,1.5,0,0));
    }
    @Test void openingCannotBeFilledByCombinedShapeBounds(){
        assertFalse(BlockShadow.covers(1,1,-1,1.1,2,1,4,-.2,-.2,5,.2,.2,0,0,0));
        assertFalse(BlockShadow.covers(1,-2,-1,1.1,-1,1,4,-.2,-.2,5,.2,.2,0,0,0));
    }
    @Test void nonFiniteDataFailsOpen(){assertFalse(behind(Double.NaN,-.2,.2));}
}
