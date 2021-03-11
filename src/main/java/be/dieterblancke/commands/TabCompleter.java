package be.dieterblancke.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TabCompleter
{

    private static final List<String> EMPTY = Collections.unmodifiableList( new ArrayList<>() );

    public static List<String> buildTabCompletion( final Collection<String> coll, final String[] args )
    {
        if ( args.length == 0 )
        {
            return new ArrayList<>( coll );
        }
        else
        {
            final String lastWord = args[args.length - 1];
            final List<String> list = new ArrayList<>();

            for ( String s : coll )
            {
                if ( s.toLowerCase().startsWith( lastWord.toLowerCase() ) )
                {
                    list.add( s );
                }
            }

            return list;
        }
    }

    public static List<String> empty()
    {
        return EMPTY;
    }
}
