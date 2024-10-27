from uuid import uuid4
from sqlalchemy import Column, Integer, String, Boolean
from sqlalchemy.ext.declarative import declarative_base
from safedrive.database.base import Base

def generate_uuid():
 return str(uuid4())  # Generates a UUID string

class NLGReport(Base):
    """
    NLG Report Model represents a Natural Language Generation (NLG) report stored in the database.
    It captures information related to reports generated for driving behaviors using LLM models.

    Attributes:
    - id (int): The unique identifier for the NLG report.
    - user_id (str): The ID of the user who received the report.
    - report_text (str): The content of the generated report.
    - date_range (str): The date range for which the report is applicable.
    - synced (bool): Boolean indicating if the report has been synced with the server.
    """
   
    __tablename__ = 'nlg_report'

    id = Column(Integer, primary_key=True, autoincrement=True, default=generate_uuid)
    user_id = Column(String(255), nullable=False)
    report_text = Column(String(2000), nullable=False)
    date_range = Column(String(255), nullable=False)
    synced = Column(Boolean)

    def __repr__(self):
        return f"<NLGReport(id={self.id}, user_id='{self.user_id}', synced={self.synced})>"