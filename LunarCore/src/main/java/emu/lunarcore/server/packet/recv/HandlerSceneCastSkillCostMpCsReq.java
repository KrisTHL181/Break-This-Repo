package emu.lunarcore.server.packet.recv;

import emu.lunarcore.proto.SceneCastSkillCostMpCsReqOuterClass.SceneCastSkillCostMpCsReq;
import emu.lunarcore.server.game.GameSession;
import emu.lunarcore.server.packet.CmdId;
import emu.lunarcore.server.packet.Opcodes;
import emu.lunarcore.server.packet.PacketHandler;
import emu.lunarcore.server.packet.send.PacketSceneCastSkillCostMpScRsp;

@Opcodes(CmdId.SceneCastSkillCostMpCsReq)
public class HandlerSceneCastSkillCostMpCsReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] data) throws Exception {
        var req = SceneCastSkillCostMpCsReq.parseFrom(data);

        session.send(new PacketSceneCastSkillCostMpScRsp(req.getAttackedGroupId()));
    }

}
