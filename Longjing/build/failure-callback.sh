#!/bin/bash

if [[ $STEP_REPO_CHECKOUT_OUTCOME == 'failure' ]]; then
  FAILURE_DETAILS='本次构建出错的原因是：仓库检出失败。\n这通常是网络波动导致的，但历史上偶尔也有填入错误仓库信息导致构建在这一步失败的先例。'
#elif [[ $STEP_LICENSE_CHECK_OUTCOME == 'failure' ]]; then
#  FAILURE_DETAILS=''
elif [[ $STEP_BUILDING_OUTCOME == 'failure' ]]; then
  FAILURE_DETAILS='本次构建出错的原因是：模组文件未能成功打包。\n这通常意味着您的作品未能通过编译。'
elif [[ $STEP_METADTA_VALIDATION_OUTCOME == 'failure' ]]; then
  FAILURE_DETAILS='本次构建出错的原因是：未能通过模组元数据检查。\n通常，这意味着您的作品可能不是有效的基于 NeoForge 的模组，也可能是模组文件总体积超过上限，但未取得许可。'
#elif [[ $STEP_PRE_DSLT_OUTCOME == 'failure' ]]; then
#  FAILURE_DETAILS=''
elif [[ $STEP_DSLT_OUTCOME == 'failure' ]]; then
  FAILURE_DETAILS='本次构建出错的原因是：未能通过服务器启动测试（Dedicated Server Launch Test，DSLT）。\n通常，这意味着您的作品没有正确隔离客户端独有的代码，但有时龙井自身问题也会致使作品在无法通过此阶段检查。'
elif [[ $STEP_PUBLISH_OUTCOME == 'failure' ]]; then
  FAILURE_DETAILS='本次构建出错的原因是：模组文件本身上传失败。\n这通常是网络波动导致的；该问题会自行缓解，您无需采取任何操作。'
else
  FAILURE_DETAILS='本次构建因龙井自身问题出错，请联系执行委员会寻求帮助。'
fi

cat > payload.json <<MSG
{
    "subject": "[$CONTEST_TITLE] $TEAM_ID 构建失败通知",
    "msg": "「$CONTEST_TITLE」参与者您好：\n\n您的作品（模组 ID $TEAM_ID）在最新一次构建（第 $GITHUB_RUN_NUMBER 次构建）中出错，未能产出有效的 jar 文件。\n$FAILURE_DETAILS\n您可以通过该 URL 获得构建详情：$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID \n如有疑问，请联系 TeaCon 执行委员会成员。\n\n\n顺颂时祺\nTeaCon 执行委员会",
    "teams": [ $TEAM_SEQ ]
}
MSG

curl -s -X POST -d @payload.json -H "Authorization: Bearer $BILUOCHUN_TOKEN" $BILUOCHUN_URL/api/v2/notify