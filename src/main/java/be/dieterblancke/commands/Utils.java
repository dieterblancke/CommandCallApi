package be.dieterblancke.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class Utils
{

    private static Field commandMapField;

    static
    {
        try
        {
            commandMapField = Bukkit.getServer().getClass().getDeclaredField( "commandMap" );
            commandMapField.setAccessible( true );
        }
        catch ( NoSuchFieldException e )
        {
            e.printStackTrace();
        }
    }

    private Utils()
    {
        // empty constructor
    }

    /**
     * Copies all elements from the iterable collection of originals to the
     * collection provided.
     *
     * @param <T>        the collection of strings
     * @param token      String to search for
     * @param originals  An iterable collection of strings to filter.
     * @param collection The collection to add matches to
     * @return the collection provided that would have the elements copied
     * into
     * @throws UnsupportedOperationException if the collection is immutable
     *                                       and originals contains a string which starts with the specified
     *                                       search string.
     * @throws IllegalArgumentException      if any parameter is is null
     * @throws IllegalArgumentException      if originals contains a null element.
     */
    protected static <T extends Collection<? super String>> T copyPartialMatches( final String token, final Iterable<String> originals, final T collection )
    {
        for ( String string : originals )
        {
            if ( startsWithIgnoreCase( string, token ) )
            {
                collection.add( string );
            }
        }

        return collection;
    }


    /**
     * This method uses a region to check case-insensitive equality. This
     * means the internal array does not need to be copied like a
     * toLowerCase() call would.
     *
     * @param string String to check
     * @param prefix Prefix of string to compare
     * @return true if provided string starts with, ignoring case, the prefix
     * provided
     * @throws NullPointerException     if prefix is null
     * @throws IllegalArgumentException if string is null
     */
    protected static boolean startsWithIgnoreCase( final String string, final String prefix )
    {
        if ( string.length() < prefix.length() )
        {
            return false;
        }
        return string.regionMatches( true, 0, prefix, 0, prefix.length() );
    }

    /**
     * This method creates a List containing a list of all online players.
     *
     * @return List of online players
     */
    protected static List<String> getOnlinePlayerList()
    {
        final List<String> players = new ArrayList<>();

        for ( Player player : Bukkit.getOnlinePlayers() )
        {
            players.add( player.getName() );
        }

        return players;
    }

    /**
     * Registers a command using reflection in the Bukkit SimpleCommandMap
     *
     * @param command The command instance to be registered.
     * @throws IllegalAccessException if the commandMap field is inaccessible.
     * @throws NoSuchFieldException   if the knownCommands field was not found.
     */
    static void registerCommand( final Command command ) throws IllegalAccessException, NoSuchFieldException
    {
        final CommandMap commandMap = getCommandMap();

        unregisterCommands( command.getName(), command.getAliases() );

        commandMap.register( command.getName(), command );
        Bukkit.getLogger().info( "Successfully registered " + command.getName() + " command!" );
    }

    /**
     * @param name       The name of the command that needs to be unregistered.
     * @param aliases    The list of aliases that need to be unregistered.
     * @throws NoSuchFieldException   if the knownCommands field was not found.
     * @throws IllegalAccessException if the knownCommands or commandMap field is inaccessible.
     */
    @SuppressWarnings( "unchecked" )
    static void unregisterCommands( final String name, final List<String> aliases ) throws NoSuchFieldException, IllegalAccessException
    {
        final CommandMap commandMap = getCommandMap();
        Field knownCommandsField;

        try
        {
            knownCommandsField = commandMap.getClass().getDeclaredField( "knownCommands" );
        }
        catch ( NoSuchFieldException e )
        {
            knownCommandsField = commandMap.getClass().getSuperclass().getDeclaredField( "knownCommands" );
        }
        knownCommandsField.setAccessible( true );

        final Map<String, Command> commands = (Map<String, Command>) knownCommandsField.get( commandMap );

        commands.remove( name );

        for ( String alias : aliases )
        {
            commands.remove( alias );
        }

        knownCommandsField.set( commandMap, commands );
    }

    private static CommandMap getCommandMap() throws IllegalAccessException
    {
        return (CommandMap) commandMapField.get( Bukkit.getServer() );
    }
}
