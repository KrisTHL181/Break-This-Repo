package emu.lunarcore.command.commands;

import emu.lunarcore.GameConstants;
import emu.lunarcore.command.Command;
import emu.lunarcore.command.CommandArgs;
import emu.lunarcore.command.CommandHandler;
import emu.lunarcore.data.GameData;
import emu.lunarcore.game.player.Player;
import emu.lunarcore.game.player.PlayerGender;
import emu.lunarcore.server.packet.send.PacketGetBasicInfoScRsp;

@Command(label = "gender", permission = "player.gender", requireTarget = true, desc = "/gender {male | female}. Sets the player gender.")
public class GenderCommand implements CommandHandler {

    @Override
    public void execute(CommandArgs args) {
        // Set world level
        Player target = args.getTarget();
        
        // Get new gender
        String gender = args.get(0).toLowerCase();
        PlayerGender playerGender = switch (gender) {
            case "m", "male", "boy", "man", "1" -> PlayerGender.GENDER_MAN;
            case "f", "female", "girl", "woman", "2" -> PlayerGender.GENDER_WOMAN;
            default -> null;
        };
        
        // Change gender
        if (playerGender != null && playerGender != target.getGender()) {
            // Get first hero excel that matches our new player gender
            var excel = GameData.getMultiplePathAvatarExcelMap().values().stream()
                    .filter(path -> path.getBaseAvatarID() == GameConstants.TRAILBLAZER_AVATAR_ID && path.getGender() == playerGender)
                    .findFirst()
                    .orElse(null);
            
            // Sanity check. Should never happen
            if (excel == null) {
                args.sendMessage("Error: No avatar path was found for this gender");
                return;
            }
            
            // Set our main character's path
            target.setAvatarPath(excel.getId());
            
            // Send packet to update our gender
            target.sendPacket(new PacketGetBasicInfoScRsp(target));
            
            // Send response message
            args.sendMessage("Gender for " + target.getName() + " set successfully");
        } else {
            args.sendMessage("Error: Invalid input");
        }
    }
}