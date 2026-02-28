package com.massivecraft.massivecore.command;

public class MassiveCommandDeprecated extends MassiveCommand
{
    // -------------------------------------------- //
    // FIELDS
    // -------------------------------------------- //
    
    public MassiveCommand target;
    
    // -------------------------------------------- //
    // CONSTRUCT
    // -------------------------------------------- //
    
    public MassiveCommandDeprecated(MassiveCommand target, String... aliases)
    {
        // Fields
        this.target = target;
        
        // Aliases
        this.setAliases(aliases);
        
        // Parameters
        this.setOverflowSensitive(false);
        
        // Visibility
        this.setVisibility(Visibility.INVISIBLE);
        
        // Priority
        this.setPriority(-1);
    }
    
    // -------------------------------------------- //
    // OVERRIDE
    // -------------------------------------------- //
    
    @Override
    public void perform()
    {   
        msg("Â§eComando nÃ£o encontrado.");
        msg("Â§eUse Â§6/fÂ§e para abrir o menu de ajuda.");
    }
    
}