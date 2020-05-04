package me.tepis.integratednbt;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class NBTExtractorRemoteRequestMessage implements IMessage {
    public static class NBTExtractorRemoteRequestMessageHandler
        implements IMessageHandler<NBTExtractorRemoteRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(NBTExtractorRemoteRequestMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            NBTExtractorRemote remote = NBTExtractorRemote.getInstance();
            if (player.getHeldItem(EnumHand.MAIN_HAND).getItem() == remote) {
                NBTExtractorRemote.getInstance()
                    .serverUse(player.getHeldItem(EnumHand.MAIN_HAND), player);
            } else if (player.getHeldItem(EnumHand.OFF_HAND).getItem() == remote) {
                NBTExtractorRemote.getInstance()
                    .serverUse(player.getHeldItem(EnumHand.OFF_HAND), player);
            }
            return null;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}
}
