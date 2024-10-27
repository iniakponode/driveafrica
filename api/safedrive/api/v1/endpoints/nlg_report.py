from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.nlg_report import NLGReportCreate, NLGReportUpdate, NLGReportBase as NLGReport
from safedrive.crud.nlg_report import nlg_report_crud

router = APIRouter()

# Endpoint to create a new NLG report
@router.post("/nlg_reports/", response_model=NLGReport)
def create_nlg_report(*, db: Session = Depends(get_db), nlg_report_in: NLGReportCreate) -> NLGReport:
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not nlg_report_in.userId or not nlg_report_in.reportText:
            raise HTTPException(status_code=400, detail="User ID and report text are required")
        return nlg_report_crud.create(db=db, obj_in=nlg_report_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating NLG report: {str(e)}")

# Endpoint to get an NLG report by ID
@router.get("/nlg_reports/{report_id}", response_model=NLGReport)
def get_nlg_report(report_id: int, db: Session = Depends(get_db)) -> NLGReport:
    try:
        nlg_report = nlg_report_crud.get(db=db, id=report_id)
        if not nlg_report:
            raise HTTPException(status_code=404, detail="NLG report not found")
        return nlg_report
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving NLG report: {str(e)}")

# Endpoint to get all NLG reports with optional pagination
@router.get("/nlg_reports/", response_model=List[NLGReport])
def get_all_nlg_reports(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[NLGReport]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return nlg_report_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving NLG reports: {str(e)}")

# Endpoint to update an existing NLG report
@router.put("/nlg_reports/{report_id}", response_model=NLGReport)
def update_nlg_report(report_id: int, *, db: Session = Depends(get_db), nlg_report_in: NLGReportUpdate) -> NLGReport:
    try:
        nlg_report = nlg_report_crud.get(db=db, id=report_id)
        if not nlg_report:
            raise HTTPException(status_code=404, detail="NLG report not found")
        return nlg_report_crud.update(db=db, db_obj=nlg_report, obj_in=nlg_report_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating NLG report: {str(e)}")

# Endpoint to delete an NLG report by ID
@router.delete("/nlg_reports/{report_id}", response_model=NLGReport)
def delete_nlg_report(report_id: int, db: Session = Depends(get_db)) -> NLGReport:
    try:
        nlg_report = nlg_report_crud.get(db=db, id=report_id)
        if not nlg_report:
            raise HTTPException(status_code=404, detail="NLG report not found")
        return nlg_report_crud.delete(db=db, id=report_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting NLG report: {str(e)}")