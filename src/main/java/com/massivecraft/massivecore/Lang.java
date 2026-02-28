package com.massivecraft.massivecore;

import com.massivecraft.massivecore.mson.Mson;
import org.bukkit.ChatColor;

import static com.massivecraft.massivecore.mson.Mson.mson;

public class Lang
{
    public static final String PERM_DEFAULT_DENIED_FORMAT = "Â§cVocÃª nÃ£o tem permissÃ£o para fazer isso.";
    public static final String PERM_DEFAULT_DESCRIPTION = "Â§cfazer isso";
    
    public static final String COMMAND_SENDER_MUST_BE_PLAYER = "Â§cEste comando nÃ£o pode ser usado pelo console.";
    public static final String COMMAND_SENDER_MUSNT_BE_PLAYER = "Â§cEste comando nÃ£o pode ser usado por jogadores.";
    public static final String COMMAND_TITLES_MUST_BE_AVAILABLE = "Â§cEste comando requer o Minecraft versÃ£o 1.8 pois ele usa titles.";
    
    public static final String COMMAND_TOO_FEW_ARGUMENTS = "Â§iVocÃª usou argumentos invÃ¡lidos para este comando.";
    public static final String COMMAND_TOO_MANY_ARGUMENTS = "Â§iVocÃª usou muitos argumentos para este comando.";
    public static final String COMMAND_TOO_MANY_ARGUMENTS2 = "Â§iTente usar Â§6/f ajuda Â§epara obter ajuda.";
    
    public static final Mson COMMAND_REPLACEMENT = mson("REPLACEMENT").color(ChatColor.YELLOW);
    
    public static final Mson COMMAND_CHILD_AMBIGUOUS = mson("Â§eComando nÃ£o encontrado.").color(ChatColor.YELLOW);
    public static final Mson COMMAND_CHILD_NONE = mson("Â§eComando nÃ£o encontrado.").color(ChatColor.YELLOW);
    public static final Mson COMMAND_CHILD_HELP = mson("Â§eUse Â§6/f Â§epara abrir o menu de ajuda.").color(ChatColor.YELLOW);
    
    public static final String COMMAND_TOO_MANY_TAB_SUGGESTIONS = "Â§cHÃ¡ Â§c%d Â§cpossibilidades de auto-completamento para isto. Tente ser mais especÃ­fico.";
}