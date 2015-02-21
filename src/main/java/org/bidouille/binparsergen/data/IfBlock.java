package org.bidouille.binparsergen.data;

import org.bidouille.binparsergen.template.IndentPrintWriter;

public class IfBlock extends DataBlock {
    private String condition;

    public IfBlock( DataBlock parent, String condition ) {
        super( parent );
        this.condition = condition;
    }

    @Override
    public void writeExtracts( IndentPrintWriter writer ) {
        writer.print( "if(" );
        writer.print( condition );
        writer.println( ") {" );
        writer.pushIndent( "    " );
        super.writeExtracts( writer );
        writer.popIndent();
        writer.println( "}" );
    }

    @Override
    public void writeStrings( IndentPrintWriter writer ) {
        writer.print( "if(" );
        writer.print( condition );
        writer.println( ") {" );
        writer.pushIndent( "    " );
        super.writeStrings( writer );
        writer.popIndent();
        writer.println( "}" );
    }
}
