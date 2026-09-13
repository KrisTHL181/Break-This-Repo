package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputDivisorStateCreateInfoEXT;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDivisorDescriptionEXT;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo.Buffer;

public final class VulkanRenderPipeline implements CompiledRenderPipeline, Destroyable {
   private final VulkanDevice device;
   private final RenderPipeline info;
   private final long withDepthPipeline;
   private final long withoutDepthPipeline;
   private final long pipelineLayout;
   private final VulkanBindGroupLayout layout;
   private final long vertexModule;
   private final long fragmentModule;
   private boolean closed;

   public VulkanRenderPipeline(
      final VulkanDevice device,
      final RenderPipeline info,
      final long withDepthPipeline,
      final long withoutDepthPipeline,
      final long pipelineLayout,
      final VulkanBindGroupLayout layout,
      final long vertexModule,
      final long fragmentModule
   ) {
      this.info = info;
      this.device = device;
      this.withDepthPipeline = withDepthPipeline;
      this.withoutDepthPipeline = withoutDepthPipeline;
      this.pipelineLayout = pipelineLayout;
      this.layout = layout;
      this.vertexModule = vertexModule;
      this.fragmentModule = fragmentModule;
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.device.createCommandEncoder().queueForDestroy(this);
      }
   }

   public static VulkanRenderPipeline compile(
      final VulkanDevice device, final VulkanBindGroupLayout layout, final RenderPipeline pipeline, final long vertexModule, final long fragmentModule
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      long pipelineLayout;
      try {
         VkPipelineLayoutCreateInfo createInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default().pSetLayouts(stack.longs(layout.handle()));
         LongBuffer pointer = stack.callocLong(1);
         VulkanUtils.crashIfFailure(
            device, VK12.vkCreatePipelineLayout(device.vkDevice(), createInfo, null, pointer), "Can't create pipeline for " + pipeline.getLocation()
         );
         pipelineLayout = pointer.get(0);
         device.instance().debug().setObjectName(device.vkDevice(), 17, pipelineLayout, () -> "Pipeline layout for " + pipeline.getLocation());
      } catch (Throwable var41) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var40) {
               var41.addSuppressed(var40);
            }
         }

         throw var41;
      }

      if (stack != null) {
         stack.close();
      }

      stack = MemoryStack.stackPush();

      VulkanRenderPipeline var38;
      try {
         Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
         ByteBuffer nameMain = stack.UTF8("main");
         VkPipelineShaderStageCreateInfo vertexStage = VkPipelineShaderStageCreateInfo.calloc(stack)
            .sType$Default()
            .stage(1)
            .module(vertexModule)
            .pName(nameMain);
         VkPipelineShaderStageCreateInfo fragmentStage = VkPipelineShaderStageCreateInfo.calloc(stack)
            .sType$Default()
            .stage(16)
            .module(fragmentModule)
            .pName(nameMain);
         ((Buffer)((Buffer)shaderStages.put(vertexStage)).put(fragmentStage)).flip();
         VertexFormat[] vertexBindings = pipeline.getVertexFormatBindings();
         org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer vertexAttributeDescriptions = VkVertexInputAttributeDescription.calloc(
            vertexBindings.length, stack
         );
         org.lwjgl.vulkan.VkVertexInputBindingDescription.Buffer vertexBindingDescriptions = VkVertexInputBindingDescription.calloc(
            vertexBindings.length, stack
         );
         org.lwjgl.vulkan.VkVertexInputBindingDivisorDescriptionEXT.Buffer vertexBindingDivisorDescriptions = VkVertexInputBindingDivisorDescriptionEXT.calloc(
            vertexBindings.length, stack
         );
         int attribLocation = 0;

         for (int i = 0; i < vertexBindings.length; i++) {
            VertexFormat bindings = vertexBindings[i];
            if (bindings != null) {
               VkVertexInputBindingDescription bindingDescription = VkVertexInputBindingDescription.calloc(stack)
                  .binding(i)
                  .stride(bindings.getVertexSize())
                  .inputRate(bindings.getStepRate() > 0 ? 1 : 0);
               vertexBindingDescriptions.put(bindingDescription);
               if (bindings.getStepRate() > 0) {
                  VkVertexInputBindingDivisorDescriptionEXT divisorBinding = VkVertexInputBindingDivisorDescriptionEXT.calloc(stack)
                     .binding(i)
                     .divisor(bindings.getStepRate());
                  vertexBindingDivisorDescriptions.put(divisorBinding);
               }

               for (VertexFormatElement element : bindings.getElements()) {
                  VkVertexInputAttributeDescription attributeDescription = VkVertexInputAttributeDescription.calloc(stack)
                     .location(attribLocation)
                     .binding(i)
                     .offset(element.offset())
                     .format(VulkanConst.toVk(element.format()));
                  vertexAttributeDescriptions.put(attributeDescription);
                  attribLocation++;
               }
            }
         }

         vertexAttributeDescriptions.flip();
         vertexBindingDescriptions.flip();
         vertexBindingDivisorDescriptions.flip();
         VkPipelineVertexInputDivisorStateCreateInfoEXT vertexInputDivisorState = VkPipelineVertexInputDivisorStateCreateInfoEXT.calloc(stack)
            .sType$Default()
            .pVertexBindingDivisors(vertexBindingDivisorDescriptions);
         VkPipelineVertexInputStateCreateInfo vertexInputState = VkPipelineVertexInputStateCreateInfo.calloc(stack)
            .sType$Default()
            .pVertexAttributeDescriptions(vertexAttributeDescriptions)
            .pVertexBindingDescriptions(vertexBindingDescriptions);
         if (vertexInputDivisorState.vertexBindingDivisorCount() > 0) {
            vertexInputState.pNext(vertexInputDivisorState);
         }

         VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
            .sType$Default()
            .topology(VulkanConst.toVk(pipeline.getPrimitiveTopology()));
         VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc(stack)
            .sType$Default()
            .polygonMode(VulkanConst.toVk(pipeline.getPolygonMode()))
            .cullMode(pipeline.isCull() ? 2 : 0)
            .frontFace(1)
            .lineWidth(1.0F);
         VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default();
         if (pipeline.getDepthStencilState() != null) {
            rasterizationState.depthBiasEnable(
               pipeline.getDepthStencilState().depthBiasConstant() != 0.0F && pipeline.getDepthStencilState().depthBiasScaleFactor() != 0.0F
            );
            rasterizationState.depthBiasConstantFactor(pipeline.getDepthStencilState().depthBiasConstant());
            rasterizationState.depthBiasSlopeFactor(pipeline.getDepthStencilState().depthBiasScaleFactor());
            depthStencilState.depthTestEnable(true);
            depthStencilState.depthWriteEnable(pipeline.getDepthStencilState().writeDepth());
            depthStencilState.depthCompareOp(VulkanConst.toVk(pipeline.getDepthStencilState().depthTest()));
         }

         ColorTargetState[] colorTargetStates = pipeline.getColorTargetStates();
         org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState.Buffer blendAttachments = VkPipelineColorBlendAttachmentState.calloc(
            colorTargetStates.length, stack
         );

         for (ColorTargetState colorTargetState : colorTargetStates) {
            blendAttachments.colorWriteMask(colorTargetState != null ? VulkanConst.toVk(colorTargetState) : 0);
            if (colorTargetState != null && colorTargetState.blendFunction().isPresent()) {
               applyBlendInformation(blendAttachments, colorTargetState.blendFunction().get());
            }

            blendAttachments.position(blendAttachments.position() + 1);
         }

         blendAttachments.position(0);
         VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default().pAttachments(blendAttachments);
         VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default().scissorCount(1).viewportCount(1);
         VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.calloc(stack)
            .sType$Default()
            .rasterizationSamples(1)
            .sampleShadingEnable(false);
         VkPipelineDynamicStateCreateInfo dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default().pDynamicStates(stack.ints(1, 0));
         VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack).sType$Default();
         IntBuffer colorAttachmentFormats = stack.mallocInt(colorTargetStates.length);

         for (int i = 0; i < colorTargetStates.length; i++) {
            ColorTargetState colorTargetState = colorTargetStates[i];
            colorAttachmentFormats.put(i, colorTargetState != null ? VulkanConst.toVk(colorTargetState.format()) : 0);
         }

         renderingInfo.pColorAttachmentFormats(colorAttachmentFormats);
         renderingInfo.depthAttachmentFormat(126);
         org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            .sType$Default()
            .flags(0)
            .pStages(shaderStages)
            .pVertexInputState(vertexInputState)
            .pInputAssemblyState(inputAssemblyState)
            .pRasterizationState(rasterizationState)
            .pDepthStencilState(depthStencilState)
            .pColorBlendState(colorBlendState)
            .pViewportState(viewportState)
            .pMultisampleState(multisampleState)
            .pDynamicState(dynamicStateInfo)
            .layout(pipelineLayout)
            .pNext(renderingInfo);
         LongBuffer pointer = stack.callocLong(1);
         VulkanUtils.crashIfFailure(
            device, VK12.vkCreateGraphicsPipelines(device.vkDevice(), 0L, createInfo, null, pointer), "Can't compile pipeline " + pipeline.getLocation()
         );
         long withDepthPipeline = pointer.get(0);
         device.instance().debug().setObjectName(device.vkDevice(), 19, withDepthPipeline, () -> "Pipeline " + pipeline.getLocation());
         long withoutDepthPipeline;
         if (pipeline.getDepthStencilState() == null) {
            renderingInfo.depthAttachmentFormat(0);
            VulkanUtils.crashIfFailure(
               device, VK12.vkCreateGraphicsPipelines(device.vkDevice(), 0L, createInfo, null, pointer), "Can't compile pipeline " + pipeline.getLocation()
            );
            withoutDepthPipeline = pointer.get(0);
            device.instance().debug().setObjectName(device.vkDevice(), 19, withoutDepthPipeline, () -> "Pipeline " + pipeline.getLocation());
         } else {
            withoutDepthPipeline = 0L;
         }

         var38 = new VulkanRenderPipeline(device, pipeline, withDepthPipeline, withoutDepthPipeline, pipelineLayout, layout, vertexModule, fragmentModule);
      } catch (Throwable var42) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var39) {
               var42.addSuppressed(var39);
            }
         }

         throw var42;
      }

      if (stack != null) {
         stack.close();
      }

      return var38;
   }

   @Override
   public void destroy() {
      if (this.withDepthPipeline != 0L) {
         VK12.vkDestroyPipeline(this.device.vkDevice(), this.withoutDepthPipeline, null);
         VK12.vkDestroyPipeline(this.device.vkDevice(), this.withDepthPipeline, null);
         VK12.vkDestroyPipelineLayout(this.device.vkDevice(), this.pipelineLayout, null);
         VK12.vkDestroyDescriptorSetLayout(this.device.vkDevice(), this.layout.handle(), null);
         VK12.vkDestroyShaderModule(this.device.vkDevice(), this.vertexModule, null);
         VK12.vkDestroyShaderModule(this.device.vkDevice(), this.fragmentModule, null);
      }
   }

   private static void applyBlendInformation(
      final org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState.Buffer blendAttachments, final BlendFunction blendFunction
   ) {
      blendAttachments.blendEnable(true)
         .colorBlendOp(VulkanConst.toVk(blendFunction.color().op()))
         .alphaBlendOp(VulkanConst.toVk(blendFunction.alpha().op()))
         .dstAlphaBlendFactor(VulkanConst.toVk(blendFunction.alpha().destFactor()))
         .dstColorBlendFactor(VulkanConst.toVk(blendFunction.color().destFactor()))
         .srcAlphaBlendFactor(VulkanConst.toVk(blendFunction.alpha().sourceFactor()))
         .srcColorBlendFactor(VulkanConst.toVk(blendFunction.color().sourceFactor()));
   }

   @Override
   public RenderPipeline info() {
      return this.info;
   }

   public VulkanDevice device() {
      return this.device;
   }

   public long withDepthPipeline() {
      return this.withDepthPipeline;
   }

   public long withoutDepthPipeline() {
      return this.withoutDepthPipeline;
   }

   public long pipelineLayout() {
      return this.pipelineLayout;
   }

   public VulkanBindGroupLayout layout() {
      return this.layout;
   }

   public long vertexModule() {
      return this.vertexModule;
   }

   public long fragmentModule() {
      return this.fragmentModule;
   }
}
