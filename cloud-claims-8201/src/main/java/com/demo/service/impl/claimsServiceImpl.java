package com.demo.service.impl;

import com.demo.mapper.claimsMapper;
import com.demo.pojo.*;
import com.demo.service.claimsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class claimsServiceImpl implements claimsService {
    @Autowired(required = false)
    claimsMapper mapper;

    public List<Claims> getClaims() {
        List<Claims> list=new ArrayList<Claims>();
        List<Adjustment_info> adjustmentInfoList=mapper.getAdjustmentsList();
        for (Adjustment_info adj:adjustmentInfoList) {
            Claims claim=new Claims();
            claim.setAdjustmentId(adj.getAdjustmentId());
            Verify_loss_info verify=mapper.getVerifyByClaimMainId(adj.getClaimMainId());
            claim.setIsVerify(verify.getVerifyLossOpinion());
            Claim_main_info claimMain=mapper.getClaimMainByiClaimMainId(adj.getClaimMainId());
            claim.setWarrantyId(claimMain.getWarrantyId());
            Survey_loss_info survey=mapper.getSurveyByClaimMainId(adj.getClaimMainId());
            claim.setCarframeId(survey.getCarframeId());
            Warranty_info warranty=mapper.getWarranty(claimMain.getWarrantyId());
            Recognizee_info recognizee=mapper.getRecognizee(warranty.getRecognizeeId());
            claim.setReceiver(recognizee.getRecognizeeName());
            claim.setSettlementTotal(adj.getPrepayMoney());
            claim.setSignDate(claimMain.getFilingTime());
            claim.setCloseCaseTime(claimMain.getCloseCaseTime());
            claim.setClaimStatus(claimMain.getClaimStatus());
            list.add(claim);
        }
        return list;
    }

    public List<Claim_main_info> getClaimMains() {
        return mapper.getClaimMains();
    }

    public Audit getAudits(String claimMainId) {
        Audit audit=new Audit();
        PersonalLoss personalLoss=new PersonalLoss();
        OthersLoss othersLoss=new OthersLoss();
        //????????????????????????
        audit.setTrdPartyLiability(0);
        //??????????????????
        audit.setCarDamage(mapper.getCarDamage(claimMainId));
        audit.setCartotals(0);
        for (Car_damage_info carDamage:audit.getCarDamage()) {
            if(carDamage.getIsTrdPartyLiability().equals("???")){
                audit.setCartotals(audit.getCartotals()+carDamage.getLossTotal());
            }else {
                audit.setTrdPartyLiability(audit.getTrdPartyLiability()+carDamage.getLossTotal());
            }
        }
        //??????????????????
        audit.setObjDamage(mapper.getObjDamage(claimMainId));
        audit.setObjtotals(0);
        for (Object_damage_info objDamage:audit.getObjDamage()) {
            if(objDamage.getIsTrdPartyLiability().equals("???")) {
                audit.setObjtotals(audit.getObjtotals() + objDamage.getLossTotal());
            }else {
                audit.setTrdPartyLiability(audit.getTrdPartyLiability()+objDamage.getLossTotal());
            }
        }
        //??????????????????
        audit.setHumanDamage(mapper.getHumanDamage(claimMainId));
        audit.setHumantotals(0);
        for (Human_injury_info humDamage:audit.getHumanDamage()) {
            if(humDamage.getIsTrdPartyLiability().equals("???")) {
                audit.setHumantotals(audit.getHumantotals() + humDamage.getLossTotal());
            }else {
                audit.setTrdPartyLiability(audit.getTrdPartyLiability()+humDamage.getLossTotal());
            }
        }
        //??????????????????????????????
        Double ratio=mapper.getSurveyByClaimMainId(claimMainId).getDutyRatio();//????????????
        double personFch=0;//???????????????????????????
        if(ratio>=100){//?????? 20%
            personFch=20;
        }else if (ratio<100&&ratio>50){//???????????? 15%
            personFch=15;
        }else if (ratio==50){//???????????? 10%
            personFch=10;
        }else if (ratio<50&&ratio>0){//???????????? 5%
            personFch=5;
        }else {//?????? 0%
            personFch=0;
        }
        double deductibles=0;//?????????????????????
        double otherdecuctibles=0;//?????????????????????
        double personLimit=0;//????????????????????????
        double othersLimit=0;//????????????????????????
        Risk_type_amount_info personRisk=mapper.getIsSuredStateByClaimMainId(claimMainId,"???????????????");//?????????????????????????????????
        Risk_type_amount_info othersRisk=mapper.getIsSuredStateByClaimMainId(claimMainId,"??????????????????");
        if(personRisk==null){//??????????????? 100%
            deductibles=100;
        }else if(personRisk.getInsuredState()>0){//???????????? 0%
            personLimit=personRisk.getInsuredAmount();
            deductibles=0;
        }else {//????????????????????? 20%
            personLimit=personRisk.getInsuredAmount();
            deductibles=20;
        }
        if(othersRisk==null){//??????????????? 100%
            otherdecuctibles=100;
        }else if (othersRisk.getInsuredState()>0){//???????????? 0%
            othersLimit=othersRisk.getInsuredAmount();
            otherdecuctibles=0;
        }else {//????????????????????? 20%
            othersLimit=othersRisk.getInsuredAmount();
            otherdecuctibles=20;
        }
        double personShouldMoney=(audit.getCartotals()+audit.getObjtotals()+audit.getHumantotals())*((100-personFch)*0.01)*((100-deductibles)*0.01);//???????????????????????????
        if (personShouldMoney>personLimit) {
            personShouldMoney = personLimit;
        }
        double othersShouldMoney=audit.getTrdPartyLiability()*((100-otherdecuctibles)*0.01);//????????????????????????
        if (othersShouldMoney>othersLimit){
            othersShouldMoney=othersLimit;
        }
        audit.setShouldMoney(personShouldMoney+othersShouldMoney);
        //??????????????????
        PersonalLoss personalLoss1=new PersonalLoss(audit.getCartotals()+audit.getObjtotals()+audit.getHumantotals(),personFch,deductibles,personLimit,personShouldMoney);
        OthersLoss othersLoss1=new OthersLoss(audit.getTrdPartyLiability(),otherdecuctibles,othersLimit,othersShouldMoney);
        audit.setPersonalLoss(personalLoss1);
        audit.setOthersLoss(othersLoss1);
        //?????? ????????????????????? ???????????????????????????
        Adjustment_info adjustmentInfo=mapper.getAdjustMentByClaimMainId(claimMainId);
        //??????????????????????????????????????????
        if(adjustmentInfo!=null&&adjustmentInfo.getPrepayMoney()>=0){
            audit.setTplMoney(adjustmentInfo.getPrepayMoney());
        }else{
            //?????????????????????????????????????????????
            Adjustment_info adjustment_info=new Adjustment_info();
            int ai1=Integer.parseInt(mapper.getAdjustmentsList().get(mapper.getAdjustmentsList().size()-1).getAdjustmentId().substring(1))+1;
            adjustment_info.setAdjustmentId("a"+ai1);
            adjustment_info.setClaimMainId(claimMainId);
            adjustment_info.setAccidentLiabilityRatio(mapper.getSurveyByClaimMainId(claimMainId).getDutyRatio());
            adjustment_info.setCarFeeTotal(audit.getCartotals());
            adjustment_info.setMaterialFeeTotal(audit.getObjtotals());
            adjustment_info.setPeopleFeeTotal(audit.getHumantotals());
            adjustment_info.setTrdPartyLiability(audit.getTrdPartyLiability());
            adjustment_info.setSettlementTotal(audit.getCartotals()+audit.getObjtotals()+audit.getHumantotals()+audit.getTrdPartyLiability());
            adjustment_info.setShouldMoney(audit.getShouldMoney());
            adjustment_info.setPrepayMoney(0);
            adjustment_info.setAdjustmentOpinion("");
            mapper.addAdjustmentInfo(adjustment_info);
            audit.setTplMoney(0);
            //???????????????????????????
            int bi1=Integer.parseInt(mapper.getAdjustmentDetailList().get(mapper.getAdjustmentDetailList().size()-1).getAdjustmentId().substring(1))+2;
            String adjustmentDetailId1="ad"+bi1;
            mapper.addAdjustmentDetail(adjustmentDetailId1,mapper.getAdjustMentByClaimMainId(claimMainId).getAdjustmentId(),0,audit.getPersonalLoss().getLossMoney(),personLimit,personFch,deductibles,personShouldMoney);
            String adjustmentDetailId2="ad"+(Integer.parseInt(mapper.getAdjustmentDetailList().get(mapper.getAdjustmentDetailList().size()-1).getAdjustmentId().substring(1))+2);
            mapper.addAdjustmentDetail(adjustmentDetailId2,mapper.getAdjustMentByClaimMainId(claimMainId).getAdjustmentId(),1,audit.getOthersLoss().getLossMoney(),othersLimit,0,otherdecuctibles,othersShouldMoney);
        }
        return audit;
    }

    public String statusClaim(String claimMainId) {
        int num=mapper.statusClaim(claimMainId);
        String rb="???????????????";
        if (num>0){
            rb="???????????????";
        }
        return rb;
    }

    public List<Warranty_info> getWarranty(String warrantyId) {
        List<Warranty_info> list=new ArrayList<Warranty_info>();
        Warranty_info warrantyInfo=mapper.getWarranty(warrantyId);
        warrantyInfo.setPolicyholdersName(mapper.getPolicyholders(warrantyInfo.getPolicyholdersId()).getPolicyholdersName());
        warrantyInfo.setRecognizeeName(mapper.getRecognizee(warrantyInfo.getRecognizeeId()).getRecognizeeName());
        list.add(warrantyInfo);
        return list;
    }

    public String payMoney(String claimMainId, double payMoneys, String opinion) {
        int row=mapper.payMoney(claimMainId,payMoneys,opinion);
        if (row > 0) {
            mapper.updClaimMain(claimMainId);
        }
        return row>0?"????????????":"??????????????????";
    }
}
