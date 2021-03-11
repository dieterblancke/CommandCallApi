/*
 * Copyright (C) 2018 DBSoftwares - Dieter Blancke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package be.dieterblancke.commands;

import com.google.common.collect.Lists;
import lombok.Data;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Command
{

    private static final Map<Plugin, List<Command>> PLUGIN_COMMANDS = new ConcurrentHashMap<>();

    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final List<String> parameters;
    private final CommandCall command;
    private final TabCall tab;

    private CommandHolder commandHolder;
    private Plugin plugin;

    Command( final String name, final String[] aliases, final String permission, List<String> parameters, final CommandCall command, final TabCall tab )
    {
        if ( parameters == null )
        {
            parameters = Lists.newArrayList();
        }

        this.name = name;
        this.aliases = new ArrayList<>( Arrays.asList( aliases ) );
        this.permission = permission;
        this.parameters = parameters;
        this.command = command;
        this.tab = tab;
    }

    public static void unregisterAll( final Plugin plugin )
    {
        if ( !PLUGIN_COMMANDS.containsKey( plugin ) )
        {
            return;
        }
        final List<Command> commands = new ArrayList<>( PLUGIN_COMMANDS.get( plugin ) );

        for ( Command command : commands )
        {
            command.unload();
        }
    }

    public void execute( final CommandSender sender, final String[] argList )
    {
        final List<String> arguments = Lists.newArrayList();
        final List<String> parameterList = Lists.newArrayList();

        for ( String argument : argList )
        {
            if ( argument.startsWith( "-" ) && this.parameters.contains( argument ) )
            {
                parameterList.add( argument );
            }
            else
            {
                arguments.add( argument );
            }
        }

        execute( sender, arguments, parameterList );
    }

    public void execute( final CommandSender sender, final List<String> arguments, final List<String> parameters )
    {
        if ( permission != null
                && !permission.isEmpty()
                && !sender.hasPermission( permission ) )
        {
            sender.sendMessage( ChatColor.RED + "You are not allowed to execute this command!" );
            return;
        }

        try
        {
            command.onExecute( sender, arguments, parameters );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public List<String> onTabComplete( final CommandSender sender, final String[] args )
    {
        final List<String> tabCompletion = tab.onTabComplete( sender, args );

        if ( tabCompletion == null )
        {
            if ( args.length == 0 )
            {
                return Utils.getOnlinePlayerList();
            }
            else
            {
                final String lastWord = args[args.length - 1];
                final List<String> list = Lists.newArrayList();

                for ( String p : Utils.getOnlinePlayerList() )
                {
                    if ( p.toLowerCase().startsWith( lastWord.toLowerCase() ) )
                    {
                        list.add( p );
                    }
                }

                return list;
            }
        }
        return tabCompletion;
    }

    public void unload()
    {
        try
        {
            Utils.unregisterCommands( commandHolder.getName(), commandHolder.getAliases() );
        }
        catch ( NoSuchFieldException | IllegalAccessException e )
        {
            e.printStackTrace();
        }
        commandHolder = null;

        if ( command instanceof Listener )
        {
            HandlerList.unregisterAll( (Listener) this );
        }

        if ( PLUGIN_COMMANDS.containsKey( plugin ) )
        {
            PLUGIN_COMMANDS.get( plugin ).remove( this );
        }
    }

    public Command register( final Plugin plugin )
    {
        this.plugin = plugin;
        if ( commandHolder != null )
        {
            throw new RuntimeException( "This command is already registered" );
        }
        commandHolder = new CommandHolder( name, aliases );

        try
        {
            Utils.registerCommand( commandHolder );
        }
        catch ( IllegalAccessException | NoSuchFieldException e )
        {
            e.printStackTrace();
            return null;
        }

        if ( command instanceof Listener )
        {
            Bukkit.getPluginManager().registerEvents( (Listener) this, null );
        }

        PLUGIN_COMMANDS.putIfAbsent( plugin, Collections.synchronizedList( new ArrayList<>() ) );
        PLUGIN_COMMANDS.get( plugin ).add( this );

        return this;
    }

    boolean check( final List<String> args )
    {
        if ( args.isEmpty() )
        {
            return false;
        }
        if ( name.equalsIgnoreCase( args.get( 0 ) ) )
        {
            return true;
        }
        for ( String alias : aliases )
        {
            if ( alias.equalsIgnoreCase( args.get( 0 ) ) )
            {
                return true;
            }
        }
        return false;
    }

    private class CommandHolder extends org.bukkit.command.Command
    {

        public CommandHolder( final String name, final List<String> aliases )
        {
            super( name, "", "/" + name, aliases );
        }

        @Override
        public boolean execute( CommandSender sender, String alias, String[] args )
        {
            Command.this.execute( sender, args );
            return true;
        }

        @Override
        public List<String> tabComplete( CommandSender sender, String alias, String[] args )
        {
            return Command.this.onTabComplete( sender, args );
        }
    }
}